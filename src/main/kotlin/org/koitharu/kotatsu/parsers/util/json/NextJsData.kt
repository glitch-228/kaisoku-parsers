package org.koitharu.kotatsu.parsers.util.json

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.nodes.Document

/** Decode only Next's JSON push arguments; never evaluate scripts from a source. */
internal fun Document.nextJsObjects(): Sequence<JSONObject> {
    val payload = select("script").asSequence().flatMap { script ->
        pushArguments(script.data()).mapNotNull { argument ->
            runCatching { JSONArray(argument).opt(1) as? String }.getOrNull()
        }
    }.joinToString("")
    return records(payload).mapNotNull { line ->
        val json = line.substringAfter(':', "")
        if (json.startsWith('{') || json.startsWith('[')) runCatching { JSONTokener(json).nextValue() }.getOrNull()
        else null
    }.flatMap { objects(it) }
}

// RSC text records carry a UTF-8 byte length and need not end with a newline. Their text may itself
// contain newlines or JSON-looking advertising markup; it is never a JSON data record.
private fun records(payload: String): Sequence<String> = sequence {
    val bytes = payload.toByteArray(Charsets.UTF_8)
    var cursor = 0
    while (cursor < bytes.size) {
        val start = cursor
        while (cursor < bytes.size && bytes[cursor] != ':'.code.toByte() && bytes[cursor] != 10.toByte()) cursor++
        if (cursor == bytes.size) break
        if (bytes[cursor] == 10.toByte()) {
            cursor++
            continue
        }
        val content = ++cursor
        if (content == bytes.size) break
        if (bytes[content] == 'T'.code.toByte()) {
            var comma = content + 1
            while (comma < bytes.size && bytes[comma].toInt().toChar() in "0123456789abcdefABCDEF") comma++
            if (comma < bytes.size && bytes[comma] == ','.code.toByte()) {
                val size = String(bytes, content + 1, comma - content - 1, Charsets.UTF_8).toIntOrNull(16)
                    ?: break
                if (size < 0 || size > bytes.size - comma - 1) break
                cursor = comma + 1 + size
                continue
            }
        }
        while (cursor < bytes.size && bytes[cursor] != 10.toByte()) cursor++
        yield(String(bytes, start, cursor - start, Charsets.UTF_8))
        if (cursor < bytes.size) cursor++
    }
}

private fun objects(value: Any?, depth: Int = 0): Sequence<JSONObject> = sequence {
    if (depth > 64) return@sequence
    when (value) {
        is JSONObject -> {
            yield(value)
            for (key in value.keys()) yieldAll(objects(value.opt(key), depth + 1))
        }
        is JSONArray -> for (index in 0 until value.length()) yieldAll(objects(value.opt(index), depth + 1))
    }
}

// A recursive regex over large catalog strings can exhaust the JVM stack. Scan JSON arguments linearly instead.
private fun pushArguments(script: String): Sequence<String> = sequence {
    val prefix = "self.__next_f.push("
    var cursor = 0
    while (cursor < script.length) {
        val match = script.indexOf(prefix, cursor)
        if (match < 0) break
        var start = match + prefix.length
        while (start < script.length && script[start].isWhitespace()) start++
        cursor = start + 1
        if (script.getOrNull(start) != '[') continue
        var quoted = false
        var escaped = false
        var depth = 0
        for (index in start until script.length) {
            val char = script[index]
            if (quoted) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') quoted = false
            } else when (char) {
                '"' -> quoted = true
                '[', '{' -> depth++
                ']', '}' -> depth--
            }
            if (depth == 0) {
                yield(script.substring(start, index + 1))
                cursor = index + 1
                break
            }
        }
    }
}

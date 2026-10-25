package org.koitharu.kotatsu.parsers.site.mangareader.es

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.util.json.unescapeJson
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Decodes only the public reader data. Site JavaScript is never evaluated. */
internal object LectorReaderPayload {
    private const val MAX_SCRIPT = 524_288
    private const val MAX_PAYLOAD = 2_097_152
    private const val MAX_PAGES = 10_000
    private const val MAX_WORDS = 40_000

    fun images(script: String, chapterUrl: String): List<String> {
        try {
            require(script.length <= MAX_SCRIPT) { "Reader script is too large" }
            val decoded = if (script.trimStart().startsWith("eval(")) unpack(script) else script
            val blob = Regex("""ReaderBlob\s*\.\s*start\s*\(\s*(\{[^}]+})\s*\)""").find(decoded)
            val pages = if (blob != null) {
                decrypt(JSONObject(blob.groupValues[1]))
            } else {
                val marker = Regex("""ts_reader\s*\.\s*run\s*\(""").find(decoded)
                    ?: error("Reader configuration is missing")
                val payload = JSONObject(decoded.substring(marker.range.last + 1).substringBeforeLast(')'))
                payload.getJSONArray("sources").getJSONObject(0).getJSONArray("images").strings()
            }
            require(pages.isNotEmpty()) { "Reader returned no images" }
            return pages.map { image ->
                val originalUrl = if (image.startsWith("//")) "https:$image" else image
                val url = originalUrl.toHttpUrlOrNull()
                    ?: error("Reader image URL is invalid")
                require(url.username.isEmpty() && url.password.isEmpty()) { "Reader image URL has credentials" }
                require(!url.encodedPath.endsWith("/readerarea.svg")) { "Reader returned placeholder images" }
                originalUrl
            }
        } catch (error: IllegalArgumentException) {
            throw ParseException("Invalid LectorHentai reader data: ${error.message}", chapterUrl, error)
        } catch (error: org.json.JSONException) {
            throw ParseException("Invalid LectorHentai reader JSON", chapterUrl, error)
        } catch (error: java.security.GeneralSecurityException) {
            throw ParseException("Cannot decrypt LectorHentai reader data", chapterUrl, error)
        } catch (error: IllegalStateException) {
            throw ParseException("Invalid LectorHentai reader data: ${error.message}", chapterUrl, error)
        }
    }

    private fun decrypt(payload: JSONObject): List<String> {
        val decoder = Base64.getDecoder()
        val key = decoder.decode(payload.getString("k"))
        val iv = decoder.decode(payload.getString("iv"))
        val encoded = payload.getString("c")
        require(key.size == 32 && iv.size == 12) { "Invalid reader key or IV length" }
        require(encoded.length <= MAX_PAYLOAD) { "Encrypted reader data is too large" }
        val encrypted = decoder.decode(encoded)
        require(encrypted.size >= 16) { "Encrypted reader data is truncated" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val decrypted = JSONObject(cipher.doFinal(encrypted).toString(Charsets.UTF_8))
        val images = decrypted.getJSONArray("p")
        require(images.length() <= MAX_PAGES) { "Too many reader pages" }
        val skipped = decrypted.optJSONArray("s") ?: JSONArray()
        require(skipped.length() <= MAX_PAGES) { "Too many skipped pages" }
        val skipIndices = buildSet {
            for (i in 0 until skipped.length()) {
                val index = skipped.get(i)
                require(index is Int && index >= 0 && index < images.length()) { "Invalid skipped page index" }
                add(index)
            }
        }
        return (0 until images.length()).filterNot { it in skipIndices }.map { images.getString(it) }
    }

    private fun JSONArray.strings(): List<String> {
        require(length() <= MAX_PAGES) { "Too many reader pages" }
        return List(length()) { getString(it) }
    }

    private fun unpack(script: String): String {
        val arguments = PackedArguments(script)
        val payload = arguments.literal()
        arguments.comma()
        val radix = arguments.number()
        arguments.comma()
        val count = arguments.number()
        arguments.comma()
        val dictionary = arguments.literal()
        require(arguments.remaining().startsWith(".split('|')")) { "Unsupported reader dictionary" }
        require(radix in 2..62 && count in 1..MAX_WORDS) { "Invalid reader dictionary" }
        val words = dictionary.split('|')
        require(words.size <= MAX_WORDS && count <= words.size) { "Invalid reader dictionary size" }
        val output = StringBuilder(payload.length)
        var end = 0
        for (token in Regex("""\b[0-9a-zA-Z]+\b""").findAll(payload)) {
            output.append(payload, end, token.range.first)
            val index = dictionaryIndex(token.value, radix)
            output.append(index?.takeIf { it < count }?.let { words[it].takeIf(String::isNotEmpty) } ?: token.value)
            require(output.length <= MAX_PAYLOAD) { "Unpacked reader data is too large" }
            end = token.range.last + 1
        }
        output.append(payload, end, payload.length)
        require(output.length <= MAX_PAYLOAD) { "Unpacked reader data is too large" }
        return output.toString()
    }

    /** Scan quoted arguments linearly; recursive regex matching can exhaust the JVM stack. */
    private class PackedArguments(private val text: String) {
        private var cursor = Regex("""}\s*\(\s*'""").find(text)?.range?.last
            ?: error("Unsupported packed reader data")

        fun literal(): String {
            whitespace()
            require(text.getOrNull(cursor) == '\'') { "Missing reader literal" }
            val start = ++cursor
            var escaped = false
            while (cursor < text.length) {
                val ch = text[cursor++]
                if (escaped) escaped = false
                else if (ch == '\\') escaped = true
                else if (ch == '\'') return text.substring(start, cursor - 1).unescapeJson()
            }
            error("Unterminated reader literal")
        }

        fun comma() {
            whitespace()
            require(text.getOrNull(cursor++) == ',') { "Missing reader argument separator" }
        }

        fun number(): Int {
            whitespace()
            val start = cursor
            while (text.getOrNull(cursor)?.let { it in '0'..'9' } == true) cursor++
            return text.substring(start, cursor).toIntOrNull() ?: error("Invalid reader argument")
        }

        fun remaining(): String {
            whitespace()
            return text.substring(cursor)
        }

        private fun whitespace() {
            while (text.getOrNull(cursor)?.isWhitespace() == true) cursor++
        }
    }

    private fun dictionaryIndex(token: String, radix: Int): Int? {
        var result = 0
        for (ch in token) {
            val digit = when (ch) {
                in '0'..'9' -> ch - '0'
                in 'a'..'z' -> ch - 'a' + 10
                in 'A'..'Z' -> ch - 'A' + 36
                else -> return null
            }
            if (digit >= radix || result > MAX_WORDS / radix) return null
            result = result * radix + digit
            if (result >= MAX_WORDS) return null
        }
        return result
    }
}

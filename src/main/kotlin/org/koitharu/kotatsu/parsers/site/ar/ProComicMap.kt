package org.koitharu.kotatsu.parsers.site.ar

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.IOException

internal data class ProComicPiece(val url: String, val left: Int, val top: Int, val width: Int, val height: Int)
internal data class ProComicMap(val width: Int, val height: Int, val pieces: List<ProComicPiece>)

/** Validate before allocating a destination bitmap or requesting any piece. */
internal fun parseProComicMap(map: JSONObject, heapBytes: Long = Runtime.getRuntime().maxMemory()): ProComicMap {
    val dimensions = map.optJSONArray("dim") ?: throw IOException("Map dimensions missing")
    val width = dimensions.optInt(0)
    val height = dimensions.optInt(1)
    val budget = minOf(32L * 1024 * 1024, (heapBytes / 16).coerceAtLeast(1024 * 1024))
    if (width !in 1..32_768 || height !in 1..32_768 || width.toLong() * height * 4 > budget) {
        throw IOException("Mapped page exceeds the safe decoding budget")
    }
    val urls = map.optJSONArray("pieces") ?: throw IOException("Map pieces missing")
    val order = map.optJSONArray("order") ?: throw IOException("Map order missing")
    val rectangles = map.optJSONArray("rects") ?: throw IOException("Map rectangles missing")
    if (urls.length() !in 1..256 || rectangles.length() !in 1..256 || order.length() != rectangles.length()) {
        throw IOException("Invalid map piece count")
    }
    val pieces = (0 until rectangles.length()).map { index ->
        val rectangle = rectangles.optJSONObject(index) ?: throw IOException("Map rectangle missing")
        val piece = order.optInt(index, -1)
        if (piece !in 0 until urls.length()) throw IOException("Invalid map piece index")
        val url = urls.optString(piece).toHttpUrlOrNull() ?: throw IOException("Invalid map image address")
        if (!url.isHttps || url.host in setOf("procomic.pieces", "procomic.signed")) throw IOException("Unsafe map image address")
        val left = rectangle.optInt("left", -1)
        val top = rectangle.optInt("top", -1)
        val w = rectangle.optInt("width", -1)
        val h = rectangle.optInt("height", -1)
        if (left < 0 || top < 0 || w <= 0 || h <= 0 || left.toLong() + w > width || top.toLong() + h > height) {
            throw IOException("Invalid map rectangle bounds")
        }
        ProComicPiece(url.toString(), left, top, w, h)
    }
    if (pieces.sumOf { it.width.toLong() * it.height } != width.toLong() * height) throw IOException("Incomplete map coverage")
    for (i in pieces.indices) for (j in 0 until i) {
        val a = pieces[i]; val b = pieces[j]
        if (a.left < b.left + b.width && b.left < a.left + a.width && a.top < b.top + b.height && b.top < a.top + a.height) {
            throw IOException("Overlapping map rectangles")
        }
    }
    return ProComicMap(width, height, pieces)
}

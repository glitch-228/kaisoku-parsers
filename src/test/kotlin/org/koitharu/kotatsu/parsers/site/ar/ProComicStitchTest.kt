package org.koitharu.kotatsu.parsers.site.ar

import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.*
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.bitmap.Rect
import org.koitharu.kotatsu.parsers.model.MangaSource

internal class ProComicStitchTest {
    @Test fun `successful stitch draws every validated rectangle and transfers bitmap ownership once`() {
        val context = Context()
        context.httpClient.newCall(request()).execute().use { result ->
            assertEquals(200, result.code)
            assertEquals("rendered", result.body!!.string())
        }
        val destination = context.destination!!
        assertEquals(2, destination.draws)
        assertEquals(1, destination.closes)
        assertEquals(4, context.network.size)
        assertTrue(context.network.all { it.url.isHttps && it.tag(MangaSource::class.java)?.name == "PROCOMIC" })
    }

    @Test fun `failed piece releases destination and never exposes signing token in error request`() {
        val context = Context(failPiece = true)
        context.httpClient.newCall(request()).execute().use { result ->
            assertEquals(502, result.code)
            assertNull(result.request.url.fragment)
            assertNull(result.request.url.query)
            assertFalse(result.message.contains("fixture-secret"))
        }
        assertEquals(1, context.destination!!.closes)
    }

    @Test fun `oversized map fails before allocation or any piece request`() {
        val context = Context(oversized = true)
        context.httpClient.newCall(request()).execute().use { assertEquals(502, it.code) }
        assertNull(context.destination)
        assertEquals(1, context.network.size)
    }

    @Test fun `synthetic hosts with missing plans never reach dns or network`() {
        val context = Context()
        for (url in listOf("http://procomic.pieces", "http://procomic.signed")) {
            context.httpClient.newCall(Request.Builder().url(url).build()).execute().use {
                assertEquals(502, it.code)
                assertNull(it.request.url.fragment)
            }
        }
        assertTrue(context.network.isEmpty())
    }

    private fun request() = Request.Builder().url("http://procomic.pieces#" + JSONObject()
        .put("chapterId", 7).put("token", "fixture-secret").put("method", "map").put("cdnPath", "/page").put("pageIndex", 0)).build()

    private class Context(val failPiece: Boolean = false, val oversized: Boolean = false) : MangaLoaderContext() {
        val parser by lazy { ProComic(this) }
        val network = arrayListOf<Request>()
        var destination: Image? = null
        override val cookieJar = InMemoryCookieJar()
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain -> parser.intercept(chain) }
            .addInterceptor { chain ->
                val request = chain.request()
                network += request
                check(request.url.host !in setOf("procomic.pieces", "procomic.signed")) { "Synthetic host escaped" }
                val plan = if (oversized) MAP.replace("[10,20]", "[32768,32768]") else MAP
                val body = if (request.url.encodedPath.startsWith("/chapter-map-proxy-plan/")) """{"data":{"map":$plan}}""" else "image"
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(if (failPiece && request.url.host == "cdn.test") 404 else 200).message("Fixture")
                    .body(body.toResponseBody()).build()
            }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "Fixture"
        override suspend fun evaluateJs(script: String): String? = error("No JS")
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = error("No JS")
        override fun createBitmap(width: Int, height: Int): Bitmap = Image(width, height).also { destination = it }
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response {
            val input = Image(5, 20)
            val result = redraw(input)
            try { return response.newBuilder().body("rendered".toResponseBody()).build() }
            finally { (result as AutoCloseable).close(); if (result !== input) input.close() }
        }
    }
    private class Image(override val width: Int, override val height: Int) : Bitmap, AutoCloseable {
        var draws = 0
        var closes = 0
        override fun drawBitmap(sourceBitmap: Bitmap, src: Rect, dst: Rect) { check(closes == 0); draws++ }
        override fun close() { closes++ }
    }
    companion object {
        private const val MAP = """{"dim":[10,20],"pieces":["https://cdn.test/left","https://cdn.test/right"],"order":[0,1],"rects":[{"left":0,"top":0,"width":5,"height":20},{"left":5,"top":0,"width":5,"height":20}]}"""
    }
}

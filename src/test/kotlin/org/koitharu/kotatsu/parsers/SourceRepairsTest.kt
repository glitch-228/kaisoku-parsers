package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.galleryadults.all.Hentai3
import org.koitharu.kotatsu.parsers.util.generateUid

internal class SourceRepairsTest {
    @Test fun `3Hentai current cover anchor preserves saved gallery and chapter identities`() = runTest {
        val context = Context { request -> when (request.url.encodedPath) {
            "/1" -> """<div class='doujin'><a href='/d/42'><img src='/cover.jpg'><span class='title'>Fixture</span></a></div>"""
            "/d/42" -> """<h1 class='title'>Fixture</h1><a class='main-cover' href='/read/42/1/'><img src='/cover.jpg'></a>"""
            else -> """<span class='total_pages'>2</span><img class='js-main-img' src='https://cdn.test/page.jpg'>"""
        } }
        val parser = Hentai3(context)
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        val saved = entry.copy(id = 123456L)
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(saved.url, details.url)
        val chapter = details.chapters!!.single()
        assertEquals(saved.id, chapter.id)
        assertEquals("/read/42/1/", chapter.url)
        val pages = parser.getPages(chapter)
        assertEquals(listOf("/read/42/1/", "/read/42/2/"), pages.map { it.url })
        assertEquals(pages.map { parser.generateUid(it.url) }, pages.map { it.id })
        assertEquals("https://cdn.test/page.jpg", parser.getPageUrl(pages.first()))
    }

    private class Context(private val body: (Request) -> String) : MangaLoaderContext() {
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(body(chain.request()).toResponseBody()).build()
        }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "test"
        override suspend fun evaluateJs(script: String): String? = null
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap) = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

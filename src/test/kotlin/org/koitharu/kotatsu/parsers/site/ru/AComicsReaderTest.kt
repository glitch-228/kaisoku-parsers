package org.koitharu.kotatsu.parsers.site.ru

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.*
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.generateUid

internal class AComicsReaderTest {
    @Test fun `new and legacy readers preserve page URLs and IDs`() = runTest {
        for (header in listOf("<nav class='reader-navigator' data-issue-count='3'></nav>",
            "<span class='issueNumber'>1/3</span>")) {
            val parser = AComics(Context(header))
            val chapter = MangaChapter(1, "Fixture", 1f, 0,
                "https://acomics.ru/~fixture/", null, 0L, null, parser.source)
            val pages = parser.getPages(chapter)
            assertEquals((1..3).map { "https://acomics.ru/~fixture/$it" }, pages.map { it.url })
            assertEquals(pages.map { parser.generateUid(it.url) }, pages.map { it.id })
            assertEquals("https://acomics.ru/upload/fixture.png", parser.getPageUrl(pages[0]))
        }
    }

    private class Context(private val header: String) : MangaLoaderContext() {
        override val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = Unit
            override fun loadForRequest(url: HttpUrl) = emptyList<Cookie>()
        }
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("$header<img class='issue' src='/upload/fixture.png'>".toResponseBody()).build()
        }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "test"
        override suspend fun evaluateJs(script: String): String? = null
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap) = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

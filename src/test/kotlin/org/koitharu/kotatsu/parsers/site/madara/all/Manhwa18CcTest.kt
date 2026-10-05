package org.koitharu.kotatsu.parsers.site.madara.all

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.*
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.model.*

internal class Manhwa18CcTest {
    @Test fun `current details include description without changing chapter identity`() = runTest {
        val parser = Manhwa18Cc(Context())
        val manga = Manga(
            id = 123,
            title = "Fixture",
            altTitles = emptySet(),
            url = "/webtoon/fixture",
            publicUrl = "https://manhwa18.cc/webtoon/fixture",
            rating = RATING_UNKNOWN,
            contentRating = ContentRating.ADULT,
            coverUrl = null,
            tags = emptySet(),
            state = null,
            authors = emptySet(),
            source = parser.source,
        )
        val details = parser.getDetails(manga)
        assertEquals(123L, details.id)
        assertTrue(details.description!!.contains("Fixture description"))
        assertEquals("/webtoon/fixture/chapter-1?style=list", details.chapters!!.single().url)
    }

    private class Context : MangaLoaderContext() {
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val body = """<h1>Fixture</h1>
                <div class='panel-story-description'><div class='dsct'><p>Fixture description</p></div></div>
                <ul class='row-content-chapter'><li class='a-h'>
                <a href='/webtoon/fixture/chapter-1'>Chapter 1</a>
                <span class='chapter-time'>1 Oct 2026</span></li></ul>"""
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(body.toResponseBody()).build()
        }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "test"
        override suspend fun evaluateJs(script: String): String? = null
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap) = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

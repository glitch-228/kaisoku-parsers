package org.koitharu.kotatsu.parsers.site.ru

import kotlinx.coroutines.test.runTest
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaLoaderContextMock
import org.koitharu.kotatsu.parsers.SourceConfigMock
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.test_util.mangaOf

internal class DesuMeParserTest {

    @Test
    fun `current search cards keep titles covers and stable identity`() = runTest {
        val parser = DesuMeParser(DesuContext())
        val manga = parser.getList(0, SortOrder.UPDATED, MangaListFilter(query = "Homeless")).single()
        assertEquals("Бездомный", manga.title)
        assertEquals(setOf("Homeless"), manga.altTitles)
        assertEquals(parser.generateUid(6294L), manga.id)
        assertEquals("https://static.desu.uno/data/manga/covers/x120/6294.jpg", manga.coverUrl)
    }

    @Test
    fun `details use poster instead of social card and keep genre title and key separate`() = runTest {
        val parser = DesuMeParser(DesuContext())
        val manga = mangaOf(parser.source, "/manga/homeless.6294/")
        val details = parser.getDetails(manga)
        assertEquals("https://static.desu.uno/data/manga/covers/preview/6294.jpg", details.largeCoverUrl)
        assertEquals("Школа", details.tags.single().title)
        assertEquals("60-School", details.tags.single().key)
        assertEquals(manga.id, details.id)
    }

    @Test
    fun migrateLegacyApiMangaUrl() = runTest {
        val parser = MangaLoaderContextMock.newParserInstance(MangaParserSource.DESUME)
        val legacyManga = mangaOf(MangaParserSource.DESUME, "https://desu.uno/manga/api/2602").copy(
            id = parser.generateUid(2602L),
        )

        val details = parser.getDetails(legacyManga)

        assertEquals(legacyManga.id, details.id)
        assertFalse("/manga/api/" in details.url)
        assertTrue(details.url.endsWith(".2602/"))
        assertTrue(details.publicUrl.endsWith(".2602/"))
        assertFalse(details.chapters.isNullOrEmpty())
    }

    @Test
    fun `fetches pages from reader api`() = runTest {
        val context = DesuContext()
        val parser = DesuMeParser(context)

        val pages = parser.getPages(
            MangaChapter(
                id = parser.generateUid(691212L),
                title = "Chapter 510",
                number = 510f,
                volume = 11,
                url = "/manga/iron-ladies.1883/vol11/ch510/rus",
                scanlator = null,
                uploadDate = 0L,
                branch = null,
                source = parser.source,
            ),
        )

        assertEquals(
            listOf(
                "https://img3.desu.uno/manga/rus/iron_ladies/vol11_ch510/iron_ladies_vol11_ch510_p001.webp",
                "https://img3.desu.uno/manga/rus/iron_ladies/vol11_ch510/iron_ladies_vol11_ch510_p002.webp",
            ),
            pages.map { it.url },
        )
        assertEquals(parser.generateUid("https://img3.desu.uno/manga/rus/iron_ladies/vol11_ch510/iron_ladies_vol11_ch510_p001.webp"), pages.first().id)
        assertTrue(context.requestedUrls.any { it.endsWith("/api/manga/1883/chapters/691212") })
    }

    private class DesuContext : MangaLoaderContext() {

        val requestedUrls = mutableListOf<String>()

        override val cookieJar: CookieJar = CookieJar.NO_COOKIES

        override val httpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(DesuInterceptor(requestedUrls))
            .build()

        @Deprecated("Provide a base url")
        override suspend fun evaluateJs(script: String): String? = evaluateJs("", script, 10_000L)

        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null

        override fun getConfig(source: MangaSource): MangaSourceConfig = SourceConfigMock()

        override fun getDefaultUserAgent(): String = "test-agent"

        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response {
            return MangaLoaderContextMock.redrawImageResponse(response, redraw)
        }

        override fun createBitmap(width: Int, height: Int): Bitmap {
            return MangaLoaderContextMock.createBitmap(width, height)
        }
    }

    private class DesuInterceptor(
        private val requestedUrls: MutableList<String>,
    ) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url
            requestedUrls += url.toString()
            val body = when (url.encodedPath) {
                "/manga/search/" -> SEARCH_HTML
                "/manga/homeless.6294/" -> DETAILS_HTML
                "/manga/iron-ladies.1883/vol11/ch510/rus" -> CHAPTER_HTML
                "/api/manga/1883/chapters/691212" -> PAGES_JSON
                else -> error("Unexpected request: $url")
            }
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    private companion object {
        // Reduced from live desu.uno responses, 2026-09-15.
        private const val SEARCH_HTML = """
            <base href="https://desu.uno/">
            <li class="AniMangaSearchCard"><a class="AniMangaSearchCard__link" href="manga/homeless.6294/">
            <img class="AniMangaSearchCard__cover" src="https://static.desu.uno/data/manga/covers/x120/6294.jpg">
            <span class="AniMangaSearchCard__title">Бездомный</span>
            <span class="AniMangaSearchCard__subtitle">Homeless</span></a></li>
        """
        private const val DETAILS_HTML = """
            <base href="https://desu.uno/">
            <meta property="og:image" content="https://static.desu.uno/data/manga/covers/snippet/6294.jpg">
            <div id="animeView"><link itemprop="url" href="https://desu.uno/manga/homeless.6294/">
            <div class="c-poster"><img itemprop="image" src="https://static.desu.uno/data/manga/covers/preview/6294.jpg"></div>
            <div class="b-entry-info"><a itemprop="genre" href="manga/?genres=60-School">Школа</a></div></div>
        """
        private const val CHAPTER_HTML = """
            <html>
                <head>
                    <script type="text/javascript">
                        window.MangaReader = {"manga":{"id":1883},"chapter":{"id":691212},"apiBaseUrl":"/api/manga/1883"};
                    </script>
                </head>
            </html>
        """

        private const val PAGES_JSON = """
            {
                "chapter": {
                    "id": 691212,
                    "pages": [
                        {"page": 1, "url": "https://img3.desu.uno/manga/rus/iron_ladies/vol11_ch510/iron_ladies_vol11_ch510_p001.webp", "width": 1600, "height": 3734},
                        {"page": 2, "url": "https://img3.desu.uno/manga/rus/iron_ladies/vol11_ch510/iron_ladies_vol11_ch510_p002.webp", "width": 1600, "height": 3734}
                    ]
                }
            }
        """
    }
}

package org.koitharu.kotatsu.parsers.site.ru.rulib

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.*
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.model.*

internal class LibSocialDetailsTest {
    @Test fun `structured summary loads from catalog and saved seed`() = runTest {
        for (summary in listOf("\"Plain summary\"", """{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Structured summary"}]}]}""")) {
            val context = Context(summary)
            val parser = MangaLibParser(context)
            val catalog = Manga(id = 1, title = "Seed", altTitles = emptySet(), url = "1--test",
                publicUrl = "https://mangalib.me/ru/manga/1--test", rating = RATING_UNKNOWN,
                contentRating = ContentRating.SAFE, coverUrl = "cover", tags = emptySet(),
                state = null, authors = emptySet(), largeCoverUrl = null, source = parser.source)
            for (seed in listOf(catalog, catalog.copy(description = "Saved description", chapters = emptyList()))) {
                val details = parser.getDetails(seed)
                assertTrue(details.description!!.contains("summary"))
                assertEquals(seed.id, details.id)
                assertEquals(emptyList<MangaChapter>(), details.chapters)
            }
        }
    }

    @Test fun `catalog entries without cover still load details`() = runTest {
        for (cover in listOf("", ",\"cover\":null", ",\"cover\":{}")) {
            val parser = MangaLibParser(Context("\"Summary\"", cover))
            val manga = parser.getList(0, SortOrder.POPULARITY, MangaListFilter()).single()
            assertEquals("Title", manga.title)
            assertNull(manga.coverUrl)
            assertEquals("Summary", parser.getDetails(manga).description)
        }
    }

    private class Context(val summary: String, val cover: String = "") : MangaLoaderContext() {
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = if (request.url.encodedPath == "/api/manga") """{"data":[{"id":1,"slug_url":"1--test","rus_name":"Title","name":"Title"$cover}]}"""
                else if (request.url.encodedPath.endsWith("/chapters")) """{"data":[]}"""
                else """{"data":{"rus_name":"Title","genres":[],"tags":[],"authors":[],"summary":$summary}}"""
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "test"
        override suspend fun evaluateJs(script: String): String? = null
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap) = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

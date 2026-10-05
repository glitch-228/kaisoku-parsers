package org.koitharu.kotatsu.parsers.site.galleryadults.all

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.*
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.generateUid
import java.util.Locale

internal class HentaiEnvyTest {
    @Test fun `current catalog detail and reader retain source and chapter identities`() = runTest {
        val context = Context()
        val parser = HentaiEnvy(context)
        val manga = parser.getList(0, SortOrder.UPDATED, MangaListFilter()).single()
        assertEquals("/gallery/42/", manga.url)
        assertEquals(parser.generateUid("/gallery/42/"), manga.id)
        val details = parser.getDetails(manga)
        assertEquals(manga.id, details.id)
        assertEquals(setOf("Artist"), details.authors)
        assertEquals("romance", details.tags.single().key)
        val chapter = details.chapters!!.single()
        assertEquals(manga.id, chapter.id)
        assertEquals("/g/42/1/", chapter.url)
        val pages = parser.getPages(chapter)
        assertEquals(listOf("https://images.example/42/1.webp", "https://images.example/42/2.jpg"),
            pages.map { it.url })
        assertEquals(listOf(parser.generateUid("/g/42/1/"), parser.generateUid("/g/42/2/")),
            pages.map { it.id })
        assertEquals(pages, parser.getPages(chapter.copy(url = "/gallery/42/")))
        assertEquals(pages, parser.getPages(chapter.copy(url = "https://hentaienvy.com/g/42/1/")))
        assertEquals(pages.first().url, parser.getPageUrl(pages.first()))
        assertEquals(pages.first().url, parser.getPageUrl(pages.first().copy(url = "/g/42/1/")))
    }

    @Test fun `search popularity language and tag routes retain their semantics`() = runTest {
        val context = Context()
        val parser = HentaiEnvy(context)
        parser.getList(0, SortOrder.POPULARITY, MangaListFilter())
        assertEquals("/popular/", context.requests.last().url.encodedPath)
        parser.getList(0, SortOrder.UPDATED, MangaListFilter(query = "two words"))
        assertEquals("two words", context.requests.last().url.queryParameter("key"))
        parser.getList(0, SortOrder.POPULARITY, MangaListFilter(locale = Locale.ENGLISH))
        assertEquals("/language/english/popular/", context.requests.last().url.encodedPath)
        parser.getList(0, SortOrder.UPDATED,
            MangaListFilter(tags = setOf(MangaTag("Romance", "romance", parser.source))))
        assertEquals("/tag/romance/", context.requests.last().url.encodedPath)
    }

    private class Context : MangaLoaderContext() {
        val requests = mutableListOf<Request>()
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request().also(requests::add)
            val html = when {
                request.url.encodedPath.startsWith("/g/") -> """
                    <section id="readerApp" data-reader-image-base="https://images.example/42/">
                    <img id="readerImg" src="https://images.example/42/1.webp"></section>
                    <script id="readerPagesJson" type="application/json">
                    [{"page":1,"ext":"webp"},{"page":2,"ext":".jpg"},{"page":0,"ext":"jpg"}]
                    </script>
                """
                request.url.encodedPath.startsWith("/gallery/") -> """
                    <div class="hnv-gallery-details"><h1>[Artist] Fixture</h1></div>
                    <div class="hnv-gallery-cover"><img src="https://images.example/42/thumb.jpg"></div>
                    <div class="hnv-gallery-entity-group">Tags:
                    <a class="hnv-gallery-tag"><span class="hnv-gallery-tag__name">Romance</span></a></div>
                    <div class="hnv-gallery-entity-group">Artists:
                    <a class="hnv-gallery-tag"><span class="hnv-gallery-tag__name">Artist</span></a></div>
                    <div class="hnv-gallery-entity-group">Languages:
                    <a class="hnv-gallery-tag"><span class="hnv-gallery-tag__name">English</span></a></div>
                """
                else -> """
                    <article class="hnv-gallery-card">
                    <a class="hnv-gallery-card__cover" href="/gallery/42/">
                    <img src="https://images.example/42/thumb.jpg"></a>
                    <h2 class="hnv-gallery-card__title">[Artist] Fixture</h2></article>
                """
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(html.toResponseBody()).build()
        }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "test"
        override suspend fun evaluateJs(script: String): String? = null
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap) = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.all.TwokindsEnglish
import org.koitharu.kotatsu.parsers.site.all.TwokindsRussian
import org.koitharu.kotatsu.parsers.site.id.Cgbum
import org.koitharu.kotatsu.parsers.site.id.Wurmz

internal class NewNativeSourcesTest {

    @Test fun `CGBUM searches are encoded once and page requests remain independent`() = runTest {
        val context = Context { "" }
        val parser = Cgbum(context)
        parser.getListPage(2, SortOrder.UPDATED, MangaListFilter(query = "魔法 & герой"))
        assertEquals("魔法 & герой", context.requests.single().url.queryParameter("q"))
        assertEquals("2", context.requests.single().url.queryParameter("page"))
        parser.getListPage(1, SortOrder.UPDATED, MangaListFilter())
        assertEquals("/daftar-komik", context.requests.last().url.encodedPath)
        assertNull(context.requests.last().url.queryParameter("q"))
    }

    @Test fun `CGBUM reads source genre values and preserves each selected genre`() = runTest {
        val context = Context { """<input name='genres[]' value='School Life'><input name='genres[]' value='魔法'>""" }
        val parser = Cgbum(context)
        val tags = parser.getFilterOptions().availableTags
        assertEquals(setOf("School Life", "魔法"), tags.map { it.key }.toSet())
        parser.getListPage(1, SortOrder.POPULARITY, MangaListFilter(tags = tags))
        assertEquals(setOf("School Life", "魔法"), context.requests.last().url.queryParameterValues("genres[]").toSet())
        assertEquals("views", context.requests.last().url.queryParameter("sort"))
    }

    @Test fun `CGBUM details retain saved identity and adult evidence while pages include last image`() = runTest {
        val context = Context { request -> if (request.url.encodedPath == "/chapter-1") """
            <div class='page-container' data-url='https://cdn.test/1.webp'></div>
            <div class='page-container'><img src='https://cdn.test/2.webp'></div>
        """ else """
            <h1>Current title</h1><div class='chapter-grid'><a class='ch-grid-item' href='/chapter-1'>Chapter 1.5</a></div>
        """ }
        val parser = Cgbum(context)
        val saved = manga(parser.source).copy(contentRating = ContentRating.ADULT)
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(saved.url, details.url)
        assertEquals(ContentRating.ADULT, details.contentRating)
        assertEquals(1.5f, details.chapters!!.single().number)
        assertEquals(listOf("https://cdn.test/1.webp", "https://cdn.test/2.webp"),
            parser.getPages(details.chapters!!.single()).map { it.url })
    }

    @Test fun `CGBUM missing pages are an error rather than a fabricated cover page`() = runTest {
        val parser = Cgbum(Context { "<img src='/advert.jpg'>" })
        assertInstanceOf(ParseException::class.java, runCatching { parser.getPages(chapter(parser.source)) }.exceptionOrNull())
    }

    @Test fun `Wurmz keeps all genre parameters and the complete title query`() = runTest {
        val context = Context { "" }
        val parser = Wurmz(context)
        val tags = setOf(MangaTag("Action", "Action", parser.source), MangaTag("Magic", "Magic", parser.source))
        parser.getListPage(2, SortOrder.POPULARITY_WEEK, MangaListFilter(query = "Bahasa Indonesia & 魔法", tags = tags))
        val url = context.requests.single().url
        assertEquals("Bahasa Indonesia & 魔法", url.queryParameter("q"))
        assertEquals(setOf("Action", "Magic"), url.queryParameterValues("genres[]").toSet())
        assertEquals("popular_7d", url.queryParameter("sort"))
        assertEquals("2", url.queryParameter("page"))
    }

    @Test fun `Wurmz resolves relative covers and excludes lazy adverts from reader pages`() = runTest {
        var body = """<article class='comic-card'><a href='/detail/manga/series'><img src='/cover.webp'></a><h2 class='comic-title'>Series</h2></article>"""
        val parser = Wurmz(Context { body })
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        assertEquals("https://wurmz.net/cover.webp", entry.coverUrl)
        body = "<div class='reader-page'><img data-src='/1.webp'></div><img data-src='/advert.jpg'>"
        assertEquals(listOf("https://wurmz.net/1.webp"), parser.getPages(chapter(parser.source)).map { it.url })
        body = "<img data-src='/advert.jpg'>"
        assertInstanceOf(ParseException::class.java, runCatching { parser.getPages(chapter(parser.source)) }.exceptionOrNull())
    }

    @Test fun `Twokinds language archives retain stable section IDs and split chapters`() = runTest {
        for (russian in listOf(false, true)) {
            val parser: MangaParser = if (russian) TwokindsRussian(Context { archiveFixture(russian) })
                else TwokindsEnglish(Context { archiveFixture(russian) })
            val saved = manga(parser.source)
            val details = parser.getDetails(saved)
            assertEquals(saved.id, details.id)
            assertEquals(listOf(0f, 6.1f, 6.2f), details.chapters!!.map { it.number })
            assertEquals(listOf("/archive/#chapter=0", "/archive/#chapter=7", "/archive/#chapter=8"),
                details.chapters!!.map { it.url })
            assertEquals(listOf("/comic/1/", "/comic/2/"), parser.getPages(details.chapters!!.first()).map { it.url })
        }
    }

    @Test fun `Twokinds title search is local and subsequent offsets do not duplicate the single work`() = runTest {
        val context = Context { archiveFixture(false) }
        val parser = TwokindsEnglish(context)
        assertTrue(parser.getList(1, SortOrder.UPDATED, MangaListFilter()).isEmpty())
        assertTrue(parser.getList(0, SortOrder.UPDATED, MangaListFilter(query = "other work")).isEmpty())
        assertTrue(context.requests.isEmpty())
        assertEquals(1, parser.getList(0, SortOrder.UPDATED, MangaListFilter(query = "two kinds")).size)
    }

    @Test fun `Twokinds resolves full comic image rather than archive thumbnails or unrelated art`() = runTest {
        for (russian in listOf(false, true)) {
            val html = if (russian) "<div id='comic'><img src='/comic/1/page'></div>"
                else "<div class='comic'><img src='https://cdn.test/full.jpg'></div>"
            val context = Context { "<img src='/thumbnail.jpg'>$html<img src='/unrelated.jpg'>" }
            val parser: MangaParser = if (russian) TwokindsRussian(context) else TwokindsEnglish(context)
            val url = parser.getPageUrl(MangaPage(1, "/comic/1/", null, parser.source))
            assertEquals(if (russian) "https://twokinds.ru/comic/1/page" else "https://cdn.test/full.jpg", url)
        }
    }

    private fun archiveFixture(russian: Boolean): String = buildString {
        val className = if (russian) "Chapter" else "chapter"
        append("<section class='$className' data-ch-id='0'><h2>Prologue (Oct 2003 – Nov 2003)</h2><a href='/comic/1/'></a><a href='/comic/2/'></a></section>")
        for ((id, part) in listOf(7 to 1, 8 to 2)) {
            append("<section class='$className' data-ch-id='$id'><h2>")
            append(if (russian) "Глава 6 (часть $part)" else "Chapter 6, Part $part (Dec 2004 – Jan 2005)")
            append("</h2><a href='/comic/$id/'></a></section>")
        }
    }

    private fun manga(source: MangaSource) = Manga(
        id = 123456L, title = "Saved title", altTitles = emptySet(), url = "/series", publicUrl = "https://example.test/series",
        rating = RATING_UNKNOWN, contentRating = null, coverUrl = null, tags = emptySet(), state = null,
        authors = emptySet(), source = source,
    )

    private fun chapter(source: MangaSource) = MangaChapter(
        id = 1, title = "Chapter", number = 1f, volume = 0, url = "/chapter-1", scanlator = null,
        uploadDate = 0, branch = null, source = source,
    )

    private class Context(private val body: (Request) -> String) : MangaLoaderContext() {
        val requests = mutableListOf<Request>()
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            requests += chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(body(chain.request()).toResponseBody()).build()
        }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "fixture"
        override suspend fun evaluateJs(script: String): String? = null
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap) = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

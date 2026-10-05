package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.ar.Karh
import org.koitharu.kotatsu.parsers.site.en.MangaK
import org.koitharu.kotatsu.parsers.site.en.MangaTownParser
import org.koitharu.kotatsu.parsers.site.en.WitchToons
import org.koitharu.kotatsu.parsers.site.heancms.en.TempleScan
import org.koitharu.kotatsu.parsers.site.madara.en.MangaDna
import org.koitharu.kotatsu.parsers.site.ru.AComics
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.json.nextJsObjects
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/** Reduced fixtures from the public HTML/RSC contracts checked during fork intake. */
internal class SelectedForkSourcesTest {
    @Test fun `RSC decoder rejoins escaped chunks and ignores non JSON scripts`() {
        val json = JSONObject().put("series", JSONObject().put("title", "A \"quoted\" ] title"))
        val payload = "a:[\"$\",\"div\",null,$json]\n"
        val html = rscChunks(listOf(payload.take(12), payload.drop(12))) +
            "<script>self.__next_f.push([0]);self.__next_f.push([1,notJson]);alert('unused')</script>"
        assertEquals("A \"quoted\" ] title", Jsoup.parse(html).nextJsObjects()
            .first { it.has("series") }.getJSONObject("series").getString("title"))
        assertEquals(0, Jsoup.parse("<script>window.data={\"series\":{}}</script>").nextJsObjects().count())
    }

    @Test fun `Temple catalog infers rotating fields with full sorting and pagination`() = runTest {
        val list = JSONArray((1..23).map { n -> JSONObject().put("rotated_title", "Title %02d".format(n))
            .put("rotated_slug", "title-$n").put("thumbnail", "https://media.test/$n.jpg")
            .put("total_views", n).put("update_chapter", "2026-09-%02d".format(n)) })
        val ctx = Context { "<div class='grid'><a href='/comic/title-1'><h2>Title 01</h2></a></div>" +
            rsc(JSONObject().put("list", list)) }
        val parser = TempleScan(ctx)
        val first = parser.getListPage(1, SortOrder.POPULARITY, MangaListFilter())
        val second = parser.getListPage(2, SortOrder.POPULARITY, MangaListFilter())
        assertEquals(20, first.size)
        assertEquals(listOf("title-3", "title-2", "title-1"), second.map { it.url })
        assertEquals(1, ctx.requests.size)
        assertEquals(parser.generateUid("title-23"), first.first().id)
        assertEquals("title-1", parser.getListPage(1, SortOrder.ALPHABETICAL, MangaListFilter()).first().url)
    }

    @Test fun `Temple legacy saved slug chapter and page identities survive migration`() = runTest {
        val ctx = Context { url -> when (url.encodedPath) {
            "/comic/fixture" -> """<ul id='chapter-list'>
                <li><a href='/comic/fixture/chapter-2'><span>Premium</span></a></li>
                <li><a href='/comic/fixture/chapter-1'><span class='font-semibold'>Chapter 1</span></a></li>
                </ul><div id='series-synopsis-text'>Description</div>"""
            else -> rsc(JSONObject().put("ads", JSONArray(listOf("https://ads.test/logo.png")))
                .put("renamedImages", JSONArray(listOf("https://media.test/uploads/series/fixture/chapter/01.jpg"))))
        } }
        val parser = TempleScan(ctx)
        val original = manga(parser.source, "fixture").copy(id = parser.generateUid("fixture"))
        val details = parser.getDetails(original)
        val chapter = details.chapters!!.single()
        assertEquals(original.id, details.id)
        assertEquals(parser.generateUid("fixture/chapter-1"), chapter.id)
        assertEquals("/comic/fixture/chapter-1", chapter.url)
        val page = parser.getPages(chapter).single()
        assertEquals(parser.generateUid(page.url), page.id)
        assertTrue(page.url.endsWith("/01.jpg"))
    }

    @Test fun `AComics modern catalog filters details retain old about identity`() = runTest {
        val card = """<section class='serial-card'><a class='cover' href='/~fixture/'><img data-real-src='/cover.jpg'></a>
            <h2 class='title'><a>Fixture</a></h2></section>"""
        val ctx = Context { url -> if (url.encodedPath.endsWith("about"))
            "<p class='serial-about-authors'><a>Author</a></p><section class='serial-about-text'><p>One</p><p>Two</p></section>"
            else card + "<form class='catalog-filters-form'><fieldset class='categories'><label><input name='categories[]' value='3'>Fantasy</label></fieldset></form>"
        }
        val parser = AComics(ctx)
        val tag = parser.getFilterOptions().availableTags.single()
        val entry = parser.getListPage(1, SortOrder.ALPHABETICAL, MangaListFilter(tags = setOf(tag))).single()
        assertEquals("10", ctx.requests.last().queryParameter("skip"))
        assertEquals("3", ctx.requests.last().queryParameter("categories[]"))
        assertEquals("serial_name", ctx.requests.last().queryParameter("sort"))
        assertEquals("https://acomics.ru/~fixture/about", entry.url)
        assertEquals(parser.generateUid(entry.url), entry.id)
        val details = parser.getDetails(entry)
        assertEquals(setOf("Author"), details.authors)
        assertTrue(details.description!!.contains("Two"))
        assertEquals(entry.id, details.chapters!!.single().id)
        assertEquals("https://acomics.ru/~fixture/", details.chapters!!.single().url)
        parser.getListPage(0, SortOrder.UPDATED, MangaListFilter(query = "a & b"))
        assertEquals("a & b", ctx.requests.last().queryParameter("keyword"))
    }

    @Test fun `Karh catalog paginates without repeating all works`() = runTest {
        val ctx = Context { "<div id='allWorksGrid'>" + (1..32).joinToString("") { n ->
            "<div class='manga-filter-item'><a class='manga-card' href='/comic-$n' data-cats='Action'><h3 class='manga-card-title'>Comic $n</h3><span class='manga-card-rating-overlay'>4.5</span></a></div>"
        } + "</div>" }
        val parser = Karh(ctx)
        assertEquals(30, parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).size)
        assertEquals(0.9f, parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).first().rating)
        assertEquals(listOf("comic-31", "comic-32"), parser.getListPage(2, SortOrder.UPDATED, MangaListFilter()).map { it.url })
        assertTrue(parser.getListPage(3, SortOrder.UPDATED, MangaListFilter()).isEmpty())
        assertEquals("comic-32", parser.getListPage(1, SortOrder.UPDATED, MangaListFilter(query = "Comic 32")).single().url)
    }

    @Test fun `Witch chapters paginate and exclude inaccessible locked entries`() = runTest {
        val ctx = Context { url -> val page = url.queryParameter("page")?.toInt() ?: 1
            rsc(JSONObject().put("series", witchSeries()).put("totalPages", 2).put("currentPage", page)
                .put("chapters", JSONArray(if (page == 1) listOf(witchChapter(2), witchChapter(3, true)) else listOf(witchChapter(1))))) }
        val parser = WitchToons(ctx)
        val details = parser.getDetails(manga(parser.source, "fixture"))
        assertEquals(listOf(1f, 2f), details.chapters!!.map { it.number })
        assertEquals(listOf("/series/comic/fixture/chapter/1", "/series/comic/fixture/chapter/2"), details.chapters!!.map { it.url })
        assertEquals(ContentRating.ADULT, details.contentRating)
        assertEquals(2, ctx.requests.size)
    }

    @Test fun `Witch rejects stalled chapter pagination`() = runTest {
        val parser = WitchToons(Context { rsc(JSONObject().put("series", witchSeries()).put("totalPages", 2)
            .put("currentPage", 1).put("chapters", JSONArray(listOf(witchChapter(1))))) })
        try { parser.getDetails(manga(parser.source, "fixture")); fail("Expected pagination error") }
        catch (_: ParseException) { }
    }

    @Test fun `Witch reader orders public pages and rejects protected content`() = runTest {
        val ctx = Context { rsc(JSONObject().put("isUnlocked", true).put("chapter", JSONObject().put("pages",
            JSONArray(listOf(JSONObject().put("pageNumber", 2).put("imageUrl", "/2.jpg"),
                JSONObject().put("pageNumber", 1).put("imageUrl", "/1.jpg")))))) }
        val parser = WitchToons(ctx)
        val chapter = chapter(parser.source, "/series/comic/fixture/chapter/1")
        assertEquals(listOf("https://witchtoons.net/1.jpg", "https://witchtoons.net/2.jpg"), parser.getPages(chapter).map { it.url })
        for (flag in listOf("isEncrypted", "hasFragments", "hasStrips", "isRedacted")) {
            val protected = WitchToons(Context { rsc(JSONObject().put("isUnlocked", true)
                .put("chapter", JSONObject().put("pages", JSONArray(listOf(JSONObject().put(flag, true).put("imageUrl", "/1.jpg")))))) })
            try { protected.getPages(chapter); fail("Expected protected-format error") } catch (_: ParseException) { }
        }
    }

    @Test fun `MangaTown mobile fallback preserves relative page IDs`() = runTest {
        val ctx = Context { url -> if (url.host.startsWith("m."))
            "<select class='index-page'><option value='/manga/fixture/c001/1.html'>1</option><option value='/manga/fixture/c001/2.html'>2</option></select><div id='viewer'><img src='https://cdn.test/page.jpg'></div>"
            else "<div>Empty desktop reader</div>" }
        val parser = MangaTownParser(ctx)
        val pages = parser.getPages(chapter(parser.source, "/manga/fixture/c001/"))
        assertEquals(listOf("/manga/fixture/c001/1.html", "/manga/fixture/c001/2.html"), pages.map { it.url })
        assertEquals(pages.map { parser.generateUid(it.url) }, pages.map { it.id })
        assertEquals("https://cdn.test/page.jpg", parser.getPageUrl(pages.last()))
        assertEquals(listOf("www.mangatown.com", "m.mangatown.com", "www.mangatown.com", "m.mangatown.com"), ctx.requests.map { it.host })
    }

    @Test fun `MangaK author query is encoded and combines with title query`() = runTest {
        val ctx = Context { "{\"data\":{\"items\":[]}}" }
        val parser = MangaK(ctx)
        parser.getListPage(2, SortOrder.UPDATED, MangaListFilter(query = "One Piece", author = " Oda & Co "))
        assertTrue(parser.filterCapabilities.isAuthorSearchSupported)
        assertEquals("Oda & Co", ctx.requests.single().queryParameter("author"))
        assertEquals("One Piece", ctx.requests.single().queryParameter("q"))
        assertEquals("2", ctx.requests.single().queryParameter("page"))
    }

    @Test fun `existing MangaDNA gains metadata without replacing title chapter identities`() = runTest {
        val ctx = Context { """<h1 class='entry-title'>Updated title</h1><div class='summary_image'><img src='/cover.jpg'></div>
            <div class='author-content'><a>Author</a></div><span id='averagerate'>4.5</span><div class='dsct'>Description</div>
            <div class='panel-manga-chapter'><li class='a-h'><a href='/manga/fixture/chapter-1'><p>Chapter 1</p></a></li></div>""" }
        val parser = MangaDna(ctx)
        val original = manga(parser.source, "/manga/fixture").copy(id = parser.generateUid("/manga/fixture"))
        val details = parser.getDetails(original)
        assertEquals(original.id, details.id)
        assertEquals(original.url, details.url)
        assertEquals("Updated title", details.title)
        assertEquals(setOf("Author"), details.authors)
        assertEquals(0.9f, details.rating)
        assertEquals(parser.generateUid("/manga/fixture/chapter-1"), details.chapters!!.single().id)
    }

    private fun witchSeries() = JSONObject().put("slug", "fixture").put("title", "Fixture").put("isMature", true)
    private fun witchChapter(n: Int, locked: Boolean = false) = JSONObject().put("id", "chapter-$n")
        .put("number", n).put("isLocked", locked)
    private fun manga(source: MangaSource, url: String): Manga = Manga(id = 1, title = "Fixture", altTitles = emptySet(),
        url = url, publicUrl = "https://fixture.test$url", rating = RATING_UNKNOWN, contentRating = null,
        coverUrl = null, tags = emptySet(), state = null, authors = emptySet(), source = source)
    private fun chapter(source: MangaSource, url: String) = MangaChapter(2, "Fixture", 1f, 0, url, null, 0L, null, source)
    private fun rsc(json: JSONObject) = rscChunks(listOf("a:$json\n"))
    private fun rscChunks(chunks: List<String>) = chunks.joinToString("") {
        "<script>self.__next_f.push(${JSONArray(listOf(1, it))})</script>"
    }

    private class Context(private val body: (HttpUrl) -> String) : MangaLoaderContext() {
        val requests = CopyOnWriteArrayList<HttpUrl>()
        override val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = Unit
            override fun loadForRequest(url: HttpUrl) = emptyList<Cookie>()
        }
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request.url
            val html = try { body(request.url) } catch (e: Exception) { throw IOException("Fixture failed", e) }
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

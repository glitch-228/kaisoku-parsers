package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.exception.GraphQLException
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.all.MangaBallParser
import org.koitharu.kotatsu.parsers.site.all.XComic
import org.koitharu.kotatsu.parsers.site.id.Voratoon
import org.koitharu.kotatsu.parsers.site.madara.pt.HanamiHeaven
import org.koitharu.kotatsu.parsers.site.ru.rulib.MangaLibParser
import org.koitharu.kotatsu.parsers.util.generateUid
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

internal class StableSourcePortsTest {
    @Test fun `Voratoon current filters retain numeric catalog identity`() = runTest {
        val ctx = Context { """{"data":[{"id":12,"data":{"slug":"fixture","title":"Title","rating":7}}]}""" }
        val parser = Voratoon(ctx)
        val tags = setOf(MangaTag("Fantasy", "28", parser.source), MangaTag("Action", "3", parser.source))
        val manga = parser.getListPage(2, SortOrder.ALPHABETICAL, MangaListFilter(
            query = "a & b", tags = tags, states = setOf(MangaState.FINISHED), types = setOf(ContentType.MANHWA),
        )).single()
        val url = ctx.requests.single().url
        assertEquals("28,3", url.queryParameter("genreIds"))
        assertEquals("completed", url.queryParameter("status"))
        assertEquals("manhwa", url.queryParameter("format"))
        assertEquals("alphabetical", url.queryParameter("sort"))
        assertEquals("asc", url.queryParameter("sortOrder"))
        assertEquals("a & b", url.queryParameter("title"))
        assertEquals(parser.generateUid(12L), manga.id)
        assertEquals("/series/fixture", manga.url)
    }

    @Test fun `Voratoon details and old API id chapters keep saved identities`() = runTest {
        val ctx = Context { request -> when (request.url.encodedPath) {
            "/series/fixture" -> """{"data":{"id":12,"data":{"title":"Updated","rating":8,"genres":[]}}}"""
            "/series/fixture/chapters" -> """{"data":[{"id":55,"data":{"index":2}}]}"""
            else -> """{"data":{"dataImages":{"2":"https://cdn.test/2.jpg","1":"https://cdn.test/1.jpg"}}}"""
        } }
        val parser = Voratoon(ctx)
        val saved = manga(parser.source, "/series/fixture")
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        val chapter = details.chapters!!.single()
        assertEquals(parser.generateUid(55L), chapter.id)
        assertEquals("/series/fixture/chapters/2", chapter.url)
        assertEquals(listOf("https://cdn.test/1.jpg", "https://cdn.test/2.jpg"), parser.getPages(chapter).map { it.url })
    }

    @Test fun `MangaBall current GET routes preserve saved title and chapter identities`() = runTest {
        val ctx = Context { request -> when {
            request.url.encodedPath.contains("/detail/") -> """{"data":{"id":"id","name":"Updated","tags":[],"author":[],"image":{},"ratings":{"rating_average":8.5}}}"""
            request.url.encodedPath.endsWith("chapter-listing") -> """{"data":[{"id":"chapter","lang":"en","number":1,"volume":0,"name":"Chapter 1","group_name":"Team"}],"pagination":{"total_pages":1}}"""
            else -> """{"data":{"chapter":{"pages":["https://cdn.test/page.jpg"]}}}"""
        } }
        val parser = MangaBallParser.English(ctx)
        val saved = manga(parser.source, "legacy-title-id")
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(saved.url, details.url)
        assertEquals(0.85f, details.rating)
        assertTrue(ctx.requests.all { it.url.host == "mangaball.com" })
        assertTrue(ctx.requests.any { it.url.encodedPath == "/api/v1/title/detail/id" })
        val chapter = details.chapters!!.single()
        assertEquals(parser.generateUid("chapter"), chapter.id)
        assertNull(chapter.branch)
        val page = parser.getPages(chapter).single()
        assertEquals(parser.generateUid(page.url), page.id)
        assertEquals(3, ctx.requests.size)
    }

    @Test fun `MangaBall language search and alphabetic sorting are explicit`() = runTest {
        val ctx = Context { """{"data":[{"id":"id","name":"Title","image":{"cover":{"path":"id\\cover.jpg"}}}]}""" }
        val parser = MangaBallParser.Russian(ctx)
        val manga = parser.getListPage(3, SortOrder.ALPHABETICAL, MangaListFilter(query = "a & b")).single()
        val request = ctx.requests.single()
        assertEquals("GET", request.method)
        assertEquals("ru", request.url.queryParameter("language"))
        assertEquals("asc", request.url.queryParameter("sort_order"))
        assertEquals("a & b", request.url.queryParameter("keyword"))
        assertEquals("3", request.url.queryParameter("page"))
        assertEquals(parser.generateUid("id"), manga.id)
        assertTrue(manga.coverUrl!!.endsWith("/id/cover.jpg"))
    }

    @Test fun `MangaBall chapters paginate deduplicate and retain canonical translation selection`() = runTest {
        val ctx = Context { request -> when {
            request.url.encodedPath.contains("/detail/") -> """{"data":{"id":"id","name":"Title"}}"""
            request.url.queryParameter("page") == "1" -> """{"data":[{"id":"a","lang":"en","number":1,"group_name":"Team"}],"pagination":{"total_pages":2}}"""
            else -> """{"data":[{"id":"a","lang":"en","number":1,"group_name":"Team"},{"id":"b","lang":"en","number":2,"group_name":"Team"},{"id":"other","lang":"fr","number":3}],"pagination":{"total_pages":2}}"""
        } }
        val parser = MangaBallParser.English(ctx)
        assertEquals(listOf("a", "b"), parser.getDetails(manga(parser.source, "id")).chapters!!.map { it.url })
        assertEquals(listOf("1", "2"), ctx.requests.filter { it.url.encodedPath.endsWith("chapter-listing") }.map { it.url.queryParameter("page") })
    }

    @Test fun `Hanami uses verified Madara catalog AJAX and reader contracts`() = runTest {
        val ctx = Context { request -> when {
            request.url.encodedPath.endsWith("ajax/chapters/") -> """<li class='wp-manga-chapter'><a href='/manga/fixture/cap-1/'>Cap 1</a><span class='chapter-release-date'><i>01/10/2026</i></span></li>"""
            request.url.encodedPath.contains("cap-1") -> """<div class='main-col-inner'><div class='reading-content'><div class='page-break'><img src='https://cdn.test/page.jpg'></div></div></div>"""
            request.url.encodedPath.contains("fixture") -> """<h1>Fixture</h1><div class='summary__content'>Description</div><div id='manga-chapters-holder' data-id='1'></div>"""
            else -> """<div class='row c-tabs-item__content'><div class='tab-thumb'><a href='/manga/fixture/'><img src='/cover.jpg'></a></div><div class='tab-summary'><h3>Fixture</h3></div></div>"""
        } }
        val parser = HanamiHeaven(ctx)
        val entry = parser.getListPage(0, SortOrder.UPDATED, MangaListFilter()).single()
        val details = parser.getDetails(entry)
        val chapter = details.chapters!!.single()
        assertEquals(ContentRating.ADULT, details.contentRating)
        assertEquals(entry.id, details.id)
        assertEquals(parser.generateUid("/manga/fixture/cap-1/"), chapter.id)
        assertTrue(ctx.requests.any { it.method == "POST" && it.url.encodedPath.endsWith("ajax/chapters/") })
        assertEquals("https://cdn.test/page.jpg", parser.getPages(chapter).single().url)
    }

    @Test fun `XComic browsing makes one request regardless of edition count`() = runTest {
        val ctx = Context { """{"data":{"get_title_browse_items":[{"id":"title","data":{"title":"Fixture","native_title":"Original","cover_local_url":"/cover.webp","content_rating_id":"explicit"}}]}}""" }
        val parser = XComic(ctx)
        val entry = parser.getListPage(2, SortOrder.UPDATED, MangaListFilter(locale = Locale.forLanguageTag("pt-BR"))).single()
        assertEquals("title:pt_br", entry.url)
        assertEquals(ContentRating.ADULT, entry.contentRating)
        assertEquals(1, ctx.requests.size)
        val select = requestJson(ctx.requests.single()).getJSONObject("variables").getJSONObject("select")
        assertEquals(2, select.getInt("page"))
        assertEquals(30, select.getInt("init"))
        assertEquals("pt_br", select.getJSONArray("incTLangs").getString(0))
    }

    @Test fun `XComic details select requested language without probing every edition`() = runTest {
        val ctx = xcomicContext()
        val parser = XComic(ctx)
        val saved = manga(parser.source, "title:ru")
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals("title:ru:ru-edition", details.url)
        assertEquals(listOf(1f, 2f), details.chapters!!.map { it.number })
        val comicQueries = ctx.requests.filter { requestJson(it).optString("query").contains("get_comicNode") }
        assertEquals(1, comicQueries.size)
        assertEquals("ru-edition", requestJson(comicQueries.single()).getJSONObject("variables").getString("id"))
        assertEquals(5, ctx.requests.size)
        val chapter = details.chapters!!.first()
        assertEquals(parser.generateUid("ch1"), chapter.id)
        assertEquals("https://cdn.test/page.webp", parser.getPages(chapter).single().url)
        val previousHtmlCalls = ctx.requests.count { it.method == "GET" }
        assertEquals(details.url, parser.getDetails(details).url)
        assertEquals(previousHtmlCalls, ctx.requests.count { it.method == "GET" })
    }

    @Test fun `XComic rejects GraphQL failures instead of an empty catalog`() = runTest {
        val parser = XComic(Context { """{"data":null,"errors":[{"message":"Temporary failure"}]}""" })
        try { parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()); fail("Expected error") }
        catch (e: GraphQLException) { assertTrue(e.message.orEmpty().contains("Temporary failure")) }
    }

    @Test fun `XComic rejects stalled chapter pagination`() = runTest {
        val parser = XComic(xcomicContext(stalled = true))
        try { parser.getDetails(manga(parser.source, "title:ru")); fail("Expected pagination error") }
        catch (e: ParseException) { assertTrue(e.message.orEmpty().contains("advance")) }
    }

    @Test fun `LibSocial auth is read in suspend requests and follows logout`() = runTest {
        val ctx = Context { """{"data":[{"id":1,"slug_url":"1--fixture","rus_name":"Title","name":"Title"}]}""" }
        val parser = MangaLibParser(ctx)
        ctx.auth = """{"token":{"access_token":"first"}}"""
        parser.getListPage(1, SortOrder.UPDATED, MangaListFilter())
        ctx.auth = null
        parser.getListPage(2, SortOrder.UPDATED, MangaListFilter())
        assertEquals("Bearer first", ctx.requests[0].header("Authorization"))
        assertNull(ctx.requests[1].header("Authorization"))
        assertEquals(2, ctx.authReads)
    }

    private fun xcomicContext(stalled: Boolean = false) = Context { request ->
        if (request.method == "GET") {
            """<div class='rounded-box'><span><span class='font-family-NotoColorEmoji'>🇬🇧</span><a href='/source/en-edition'>Fixture</a></span>300 chapters</div>
            <div class='rounded-box'><span><span class='font-family-NotoColorEmoji'>🇷🇺</span><a href='/source/ru-edition'>Fixture</a></span>200 chapters</div>"""
        } else {
            val json = requestJson(request)
            val query = json.getString("query")
            val vars = json.getJSONObject("variables")
            when {
                query.contains("get_title_titleNode") -> """{"data":{"get_title_titleNode":{"data":{"title":"Updated","genre_ids":[],"authors":[],"artists":[]}}}}"""
                query.contains("get_comicNode") -> """{"data":{"get_comicNode":{"data":{"isPublic":true,"dbStatus":"normal","translatedLanguage":"ru"}}}}"""
                query.contains("get_chapterNode") -> """{"data":{"get_chapterNode":{"data":{"imageUrls":["https://cdn.test/page.webp"]}}}}"""
                else -> {
                    val page = vars.getJSONObject("select").getInt("page")
                    val number = if (stalled) 2 else if (page == 1) 2 else 1
                    val next = if (stalled || page == 1) 2 else 0
                    """{"data":{"get_comic_chapterList_fullList":{"paging":{"next":$next},"items":[{"id":"ch$number","data":{"id":"ch$number","chaNum":$number,"dname":"Chapter $number"}}]}}}"""
                }
            }
        }
    }
    private fun requestJson(request: Request): JSONObject {
        val buffer = Buffer()
        request.body?.writeTo(buffer)
        return buffer.readUtf8().let { if (it.isBlank()) JSONObject() else JSONObject(it) }
    }
    private fun manga(source: MangaSource, url: String) = Manga(
        id = 42, title = "Saved", altTitles = emptySet(), url = url, publicUrl = "https://fixture.test/$url",
        rating = RATING_UNKNOWN, contentRating = null, coverUrl = null, tags = emptySet(), state = null,
        authors = emptySet(), source = source,
    )
    private class Context(private val body: (Request) -> String) : MangaLoaderContext() {
        val requests = CopyOnWriteArrayList<Request>()
        var auth: String? = null
        var authReads = 0
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(body(request).toResponseBody()).build()
        }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "test"
        override suspend fun evaluateJs(script: String): String? = null
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? {
            authReads++
            return auth
        }
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap) = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.madtheme.en.ToonilyMe
import org.koitharu.kotatsu.parsers.util.generateUid

internal class ToonilyMigrationTest {
    @Test fun `API listing retains URL identities and complete independently encoded filters`() = runTest {
        val context = Context { listFixture }
        val parser = ToonilyMe(context)
        val tags = setOf(MangaTag("School Life", "school-life", parser.source), MangaTag("Magic", "magic", parser.source))
        val entry = parser.getListPage(2, SortOrder.POPULARITY, MangaListFilter(query = "魔法 & жизнь?!", tags = tags,
            states = setOf(MangaState.FINISHED))).single()
        assertEquals("TOONILY_ME", entry.source.name)
        assertEquals(parser.generateUid("/series"), entry.id)
        assertNotEquals(parser.generateUid("opaque-api-id"), entry.id)
        assertEquals("/series", entry.url)
        val url = context.requests.single().url
        assertEquals("api.toontop.io", url.host)
        assertEquals("2", url.queryParameter("page"))
        assertEquals("views", url.queryParameter("sort"))
        assertEquals("completed", url.queryParameter("status"))
        assertEquals("魔法 & жизнь?!", url.queryParameter("q"))
        assertEquals("school-life,magic", url.queryParameter("genres"))
        parser.getListPage(1, SortOrder.UPDATED, MangaListFilter())
        assertNull(context.requests.last().url.queryParameter("q"))
        assertEquals("1", context.requests.last().url.queryParameter("page"))
    }

    @Test fun `saved title and uniquely confirmed legacy chapter URL preserve their exact identities`() = runTest {
        val context = Context { request -> if (request.url.host.startsWith("api.")) chaptersFixture else titleFixture }
        val parser = ToonilyMe(context)
        val old = chapter(913, "https://toonily.me/series/chapter-1/")
        val saved = manga().copy(chapters = listOf(old), contentRating = ContentRating.ADULT)
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(saved.url, details.url)
        assertEquals(saved.publicUrl, details.publicUrl)
        assertEquals(old.id, details.chapters!!.first().id)
        assertEquals(old.url, details.chapters!!.first().url)
        assertEquals(listOf(1f, 1.5f, 3f), details.chapters!!.map { it.number })
        assertEquals(setOf("Alias"), details.altTitles)
        assertEquals(setOf("Author", "Artist"), details.authors)
        assertEquals(ContentRating.ADULT, details.contentRating)
        assertEquals("https://toontop.io/series/", context.requests.first().url.toString())
        assertEquals("/titles/opaque-api-id/chapters", context.requests.last().url.encodedPath)
        assertEquals("123", context.requests.last().url.queryParameter("cv"))
        assertEquals(parser.generateUid("/series/chapter-1.5"), details.chapters!![1].id)
    }

    @Test fun `stored former default routes to the current host without overwriting configuration`() = runTest {
        val context = Context("toonily.me") { listFixture }
        val parser = ToonilyMe(context)
        parser.getListPage(1, SortOrder.UPDATED, MangaListFilter())
        assertEquals("api.toontop.io", context.requests.single().url.host)
        assertEquals("toonily.me", parser.config[parser.configKeyDomain])
        assertEquals("https://toontop.io/", parser.getRequestHeaders()["Referer"])
    }

    @Test fun `custom mirror retains original Madtheme protocol and its setting`() = runTest {
        val context = Context("mirror.fixture.invalid") { "<html></html>" }
        val parser = ToonilyMe(context)
        assertTrue(parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).isEmpty())
        assertEquals("mirror.fixture.invalid", context.requests.single().url.host)
        assertEquals("/search/", context.requests.single().url.encodedPath)
        assertEquals("updated_at", context.requests.single().url.queryParameter("sort"))
        assertEquals("mirror.fixture.invalid", parser.config[parser.configKeyDomain])
    }

    @Test fun `current reader includes all explicit images and keeps exact page UID inputs without old fallback`() = runTest {
        val context = Context { readerFixture("/1.webp", "https://images.fixture.invalid/2.webp", "/1.webp") }
        val parser = ToonilyMe(context)
        val pages = parser.getPages(chapter(1, "https://toonily.me/series/chapter-1/"))
        assertEquals(listOf("https://toontop.io/1.webp", "https://images.fixture.invalid/2.webp"), pages.map { it.url })
        assertEquals(pages.map { parser.generateUid(it.url) }, pages.map { it.id })
        for (page in pages) assertEquals(page.url, parser.getPageUrl(page))
        assertEquals("toontop.io", context.requests.single().url.host)
    }

    @Test fun `missing images and unconfirmed external chapter hosts fail without fabricated pages`() = runTest {
        val context = Context { readerFixture() }
        val parser = ToonilyMe(context)
        assertInstanceOf(ParseException::class.java, runCatching { parser.getPages(chapter(1, "/series/chapter-1")) }.exceptionOrNull())
        context.requests.clear()
        assertInstanceOf(ParseException::class.java,
            runCatching { parser.getPages(chapter(1, "//unrelated.fixture.invalid/series/chapter-1")) }.exceptionOrNull())
        assertTrue(context.requests.isEmpty())
    }

    @Test fun `ambiguous old chapter URL aliases are not resolved by title or chapter number`() = runTest {
        val context = Context { request -> if (request.url.host.startsWith("api.")) chaptersFixture else titleFixture }
        val parser = ToonilyMe(context)
        val saved = manga().copy(chapters = listOf(chapter(1001, "/series/chapter-1/"), chapter(1002, "/series/chapter-1")))
        val first = parser.getDetails(saved).chapters!!.first()
        assertEquals(parser.generateUid("/series/chapter-1"), first.id)
        assertNotEquals(1001, first.id)
        assertNotEquals(1002, first.id)
    }

    @Test fun `current genre data preserves source keys`() = runTest {
        val parser = ToonilyMe(Context { """{"data":{"items":[{"slug":"school-life","name":"School Life"}]}}""" })
        val tag = parser.getFilterOptions().availableTags.single()
        assertEquals("school-life", tag.key)
        assertEquals(parser.source, tag.source)
    }

    private fun manga() = Manga(id = 9001, title = "Saved title", altTitles = emptySet(),
        url = "https://toonily.me/series/", publicUrl = "https://toonily.me/series/", rating = RATING_UNKNOWN,
        contentRating = null, coverUrl = null, tags = emptySet(), state = null, authors = emptySet(),
        source = MangaParserSource.TOONILY_ME)

    private fun chapter(id: Long, url: String) = MangaChapter(id, "Saved chapter", 1f, 0, url, null, 0L, null,
        MangaParserSource.TOONILY_ME)

    private val listFixture = """{"data":{"items":[{"id":"opaque-api-id","url":"/series","name":"Series","rating":4.0,"genres":[]}]}}"""
    private val titleFixture = next("initialManga", JSONObject("""{"id":"opaque-api-id","cv":123,"name":"Current title",
        "altNames":[{"name":"Alias"}],"authors":[{"name":"Author"}],"artists":[{"name":"Artist"}],"genres":[],"status":"Ongoing"}"""))
    private val chaptersFixture = """{"data":{"chapters":[
        {"id":"unused3","url":"/series/chapter-3","number":3},
        {"id":"unused15","url":"/series/chapter-1.5","number":1.5},
        {"id":"unused1","url":"/series/chapter-1","number":1}]}}"""
    private fun readerFixture(vararg images: String) = next("initialChapter", JSONObject(mapOf("images" to images.toList())))
    private fun next(key: String, value: JSONObject) = "<script id='__NEXT_DATA__' type='application/json'>" +
        JSONObject(mapOf("props" to mapOf("pageProps" to mapOf(key to value)))).toString() + "</script>"

    private class Context(private val domain: String? = null, private val body: (Request) -> String) : MangaLoaderContext() {
        val requests = mutableListOf<Request>()
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            requests += chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(body(chain.request()).toResponseBody()).build()
        }.build()
        override fun getConfig(source: MangaSource) = object : MangaSourceConfig {
            @Suppress("UNCHECKED_CAST")
            override fun <T> get(key: ConfigKey<T>): T =
                if (key is ConfigKey.Domain && domain != null) domain as T else key.defaultValue
        }
        override fun getDefaultUserAgent() = "fixture"
        override suspend fun evaluateJs(script: String): String? = null
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap) = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

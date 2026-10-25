package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.en.KappaBeast
import org.koitharu.kotatsu.parsers.site.en.KdtScans
import org.koitharu.kotatsu.parsers.site.en.Kagane
import org.koitharu.kotatsu.parsers.site.en.MangaHereEn
import org.koitharu.kotatsu.parsers.site.ar.LavaScans
import org.koitharu.kotatsu.parsers.site.galleryadults.all.HentaiPaw
import org.koitharu.kotatsu.parsers.site.madara.en.Manhwaz
import org.koitharu.kotatsu.parsers.site.mangareader.en.Elftoon
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.json.nextJsObjects

internal class SourceContinuationContractsTest {

    @Test fun `RSC skips byte counted multibyte text and reads adjacent JSON across push chunks`() {
        val text = "魔法\n9:{\"slides\":[{\"src\":\"https://fake.test/ad\"}]}"
        val data = "a:T${text.toByteArray().size.toString(16)},$text" +
            "b:[\"$\",null,{\"slides\":[{\"src\":\"https://cdn.test/final.webp\"}]}]\n"
        val cut = data.indexOf("slides", data.indexOf("b:")) + 3
        val document = Jsoup.parse(push(data.take(cut)) + push(data.drop(cut)))
        val objects = document.nextJsObjects().toList()
        assertEquals(listOf("https://cdn.test/final.webp"), objects.mapNotNull {
            it.optJSONArray("slides")?.getJSONObject(0)?.getString("src")
        })
    }

    @Test fun `RSC never executes text and stops at truncated text records`() {
        val doc = Jsoup.parse(push("1:Tffff,fetch('https://unrelated.test')\n2:{\"slides\":[]}:"))
        assertTrue(doc.nextJsObjects().toList().isEmpty())
        assertTrue(Jsoup.parse(push("1:")).nextJsObjects().toList().isEmpty())
    }

    @Test fun `ManhwaZ paginates authoritative chapters preserving old identities extras and final image`() = runTest {
        val context = Context { request ->
            if (request.url.encodedPath.endsWith("chapter-1")) """
                <img src='https://cdn.test/advert.webp'><div class='reader-pages'>
                <figure class='reader-page-image'><img src='https://cdn.test/1.webp'></figure>
                <figure class='reader-page-image'><img src='https://cdn.test/final.webp'></figure></div>
            """ else if (request.url.queryParameter("chapterPage") == "2") """
                <a class='release-row' href='/webtoon/fixture/chapter-1'><strong>Chapter 1</strong></a>
                <a class='release-row' href='/webtoon/fixture/notice'><strong>Notice</strong></a>
            """ else """
                <section class='profile-manga'><h1>Current title</h1></section>
                <a class='release-row' href='/webtoon/fixture/chapter-2.5'><strong>Chapter 2.5</strong></a>
                <a class='release-row' href='/webtoon/fixture/chapter-1'><strong>Chapter 1</strong></a>
                <nav class='pagination'><a aria-label='Next page' href='?chapterPage=2'>Next</a></nav>
            """
        }
        val parser = Manhwaz(context)
        val saved = manga(parser.source, "/webtoon/fixture").copy(contentRating = ContentRating.ADULT)
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(saved.url, details.url)
        assertEquals(ContentRating.ADULT, details.contentRating)
        assertEquals(listOf(1f, 2.5f, 0f), details.chapters!!.map { it.number })
        assertEquals(details.chapters!!.map { parser.generateUid(it.url) }, details.chapters!!.map { it.id })
        assertEquals(listOf("https://cdn.test/1.webp", "https://cdn.test/final.webp"),
            parser.getPages(details.chapters!!.first()).map { it.url })
    }

    @Test fun `ManhwaZ refuses repeated pagination and encodes a fresh raw search once`() = runTest {
        val context = Context { "<nav class='pagination'><a aria-label='Next page' href='?chapterPage=2'>Next</a></nav>" }
        val parser = Manhwaz(context)
        assertInstanceOf(ParseException::class.java,
            runCatching { parser.getDetails(manga(parser.source, "/webtoon/fixture")) }.exceptionOrNull())
        parser.getListPage(2, SortOrder.UPDATED, MangaListFilter(query = "魔法 & герой"))
        assertEquals("魔法 & герой", context.requests.last().url.queryParameter("s"))
        parser.getListPage(1, SortOrder.UPDATED, MangaListFilter())
        assertNull(context.requests.last().url.queryParameter("s"))
    }

    @Test fun `Elftoon retains saved identities unknown maturity locked chapters and fractional legacy URLs`() = runTest {
        val context = Context { request ->
            if (request.url.encodedPath == "/api/series") """{"data":[{"slug":"fixture","title":"Fixture"}]}"""
            else if (request.url.queryParameter("page") == "2")
                """1:{"series":{"title":"Fixture"},"chapters":[{"id":"two","number":2.5,"isLocked":true}],"totalPages":2}"""
            else """1:{"series":{"title":"Current","aliases":["Alias"]},"chapters":[{"id":"one","number":1}],"totalPages":2}"""
        }
        val parser = Elftoon(context)
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        assertEquals(parser.generateUid("/manga/fixture/"), entry.id)
        assertNull(entry.contentRating)
        assertEquals("comics", context.requests.first().url.queryParameter("contentMode"))
        val saved = entry.copy(id = 987L, contentRating = ContentRating.ADULT)
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(ContentRating.ADULT, details.contentRating)
        assertEquals(setOf("Alias"), details.altTitles)
        assertEquals(listOf("/fixture-chapter-1/", "/fixture-chapter-2-5/"), details.chapters!!.map { it.url })
        assertEquals(details.chapters!!.map { parser.generateUid(it.url) }, details.chapters!!.map { it.id })
    }

    @Test fun `Elftoon normalizes legacy and current reader URLs without dropping redacted pages`() = runTest {
        val context = Context { """1:{"chapter":{"pages":[{"imageUrl":"/first.webp"},{"imageUrl":"//cdn.test/middle.webp?token=a%2Bb"},{"imageUrl":"https://cdn.test/final.webp"}]}}""" }
        val parser = Elftoon(context)
        val legacy = chapter(parser.source, "/fixture-chapter-2-5/")
        val urls = listOf("https://elftoon.net/first.webp", "https://cdn.test/middle.webp?token=a%2Bb",
            "https://cdn.test/final.webp")
        val pages = parser.getPages(legacy)
        assertEquals(urls, pages.map { it.url })
        assertEquals(urls.map { parser.generateUid(it) }, pages.map { it.id })
        assertEquals(pages, parser.getPages(legacy.copy(url = "/series/comic/fixture/chapter/2.5")))
        assertEquals(context.requests[0].url, context.requests[1].url)
        val locked = Elftoon(Context { """1:{"chapter":{"pages":[{"imageUrl":"/a.webp"},{"isRedacted":true}]}}""" })
        assertInstanceOf(ParseException::class.java, runCatching { locked.getPages(legacy) }.exceptionOrNull())
    }

    @Test fun `Elftoon rejects missing blank or malformed URLs at every declared page position`() = runTest {
        val invalid = listOf("{}", """{"imageUrl":null}""", """{"imageUrl":""}""",
            """{"imageUrl":" \t\n "}""", """{"imageUrl":123}""", """{"imageUrl":true}""",
            """{"imageUrl":{}}""", """{"imageUrl":[]}""", """{"imageUrl":"ftp://cdn.test/page.webp"}""",
            """{"imageUrl":"https://[invalid/page.webp"}""")
        for (row in invalid) for (index in 0..2) {
            val pages = MutableList(3) { """{"imageUrl":"/page-$it.webp"}""" }.apply { this[index] = row }
            val parser = Elftoon(Context { "1:{\"chapter\":{\"pages\":[${pages.joinToString()}]}}" })
            val error = runCatching { parser.getPages(chapter(parser.source, "/fixture-chapter-1/")) }.exceptionOrNull()
            val failure = assertInstanceOf(ParseException::class.java, error, "Page ${index + 1}: $row")
            assertTrue(failure.shortMessage.orEmpty().contains("page ${index + 1}", ignoreCase = true))
            assertEquals("https://elftoon.net/series/comic/fixture/chapter/1", failure.url)
        }
    }

    @Test fun `Elftoon rejects non-object declared page entries`() = runTest {
        for (row in listOf("null", "123", "true", "[]", "\"/page.webp\"")) for (index in 0..2) {
            val pages = MutableList(3) { """{"imageUrl":"/page-$it.webp"}""" }.apply { this[index] = row }
            val parser = Elftoon(Context { "1:{\"chapter\":{\"pages\":[${pages.joinToString()}]}}" })
            val error = runCatching { parser.getPages(chapter(parser.source, "/fixture-chapter-1/")) }.exceptionOrNull()
            val failure = assertInstanceOf(ParseException::class.java, error, "Page ${index + 1}: $row")
            assertTrue(failure.shortMessage.orEmpty().contains("page ${index + 1}", ignoreCase = true))
        }
    }

    @Test fun `Elftoon rejects malformed or empty declared page containers`() = runTest {
        for (pages in listOf("null", "123", "true", "{}", "\"pages\"", "[]")) {
            val parser = Elftoon(Context { "1:{\"chapter\":{\"pages\":$pages}}" })
            assertInstanceOf(ParseException::class.java,
                runCatching { parser.getPages(chapter(parser.source, "/fixture-chapter-1/")) }.exceptionOrNull(), pages)
        }
    }

    @Test fun `Elftoon refuses locked chapters even when preview image URLs are present`() = runTest {
        for (pages in listOf("[]", """[{"imageUrl":"/preview.webp"}]""")) {
            val parser = Elftoon(Context { "1:{\"isLocked\":true,\"chapter\":{\"pages\":$pages}}" })
            val error = runCatching { parser.getPages(chapter(parser.source, "/fixture-chapter-1/")) }.exceptionOrNull()
            val failure = assertInstanceOf(ParseException::class.java, error, pages)
            assertTrue(failure.shortMessage.orEmpty().contains("locked", ignoreCase = true))
        }
    }

    @Test fun `Elftoon refuses locked or redacted pages even when every image URL is present`() = runTest {
        for (flag in listOf("isLocked", "isRedacted")) for (index in 0..2) {
            val pages = MutableList(3) { """{"imageUrl":"/page-$it.webp"}""" }.apply {
                this[index] = """{"imageUrl":"/page-$index.webp","$flag":true}"""
            }
            val parser = Elftoon(Context { "1:{\"chapter\":{\"pages\":[${pages.joinToString()}]}}" })
            val error = runCatching { parser.getPages(chapter(parser.source, "/fixture-chapter-1/")) }.exceptionOrNull()
            val failure = assertInstanceOf(ParseException::class.java, error, "$flag at page ${index + 1}")
            assertTrue(failure.shortMessage.orEmpty().contains("locked", ignoreCase = true))
        }
    }

    @Test fun `Elftoon rejects missing repeated ambiguous or invalid chapter identities`() = runTest {
        val invalid = listOf(
            """[{"number":1},{"number":2}]""",
            """[{"id":"same","number":1},{"id":"same","number":2}]""",
            """[{"id":"one","number":1},{"id":"two","number":"1.00"}]""",
            """[{"id":"one","number":"not-a-number"}]""",
            """[{"id":"one","number":"NaN"}]""",
            """[{"id":"one","number":"Infinity"}]""",
            """[{"id":"one","number":-1}]""",
        )
        for (rows in invalid) {
            val parser = Elftoon(Context { "1:{\"series\":{\"title\":\"Fixture\"},\"chapters\":$rows,\"totalPages\":1}" })
            assertInstanceOf(ParseException::class.java,
                runCatching { parser.getDetails(manga(parser.source, "/manga/fixture/")) }.exceptionOrNull(), rows)
        }
        val parser = Elftoon(Context {
            """1:{"series":{"title":"Fixture"},"chapters":[{"id":"one","number":"1.0000000000000001"},{"id":"two","number":"1.0000000000000002"}],"totalPages":1}"""
        })
        assertEquals(listOf("/fixture-chapter-1-0000000000000001/", "/fixture-chapter-1-0000000000000002/"),
            parser.getDetails(manga(parser.source, "/manga/fixture/")).chapters!!.map { it.url })
    }

    @Test fun `KdtScans streamed cards deduplicate and retain historical title and chapter identities`() = runTest {
        val context = Context { request -> if (request.url.encodedPath == "/search/") """
            <a href='/series/'>Series navigation</a><div id='S:1'>
            <a href='/series/fixture/'><h3>Fixture</h3><img src='/cover.webp'></a>
            <a href='/series/fixture/'><h3>Duplicate</h3></a></div>
        """ else """
            <h1>Fixture</h1><section><div class='columns-1'>
            <a href='/series/fixture/fixture-chapter-2-5/'><span class='text-base'>Chapter 2.5</span></a>
            <a href='/fixture-chapter-1/'><span class='text-base'>Chapter 1</span></a>
            </div></section>
        """ }
        val parser = KdtScans(context)
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        assertEquals("/fixture/", entry.url)
        assertEquals(parser.generateUid("/fixture/"), entry.id)
        val details = parser.getDetails(entry.copy(id = 987L))
        assertEquals(987L, details.id)
        assertEquals(listOf("/fixture-chapter-1/", "/fixture-chapter-2-5/"), details.chapters!!.map { it.url })
        assertEquals(listOf(1f, 2.5f), details.chapters!!.map { it.number })
        parser.getListPage(2, SortOrder.UPDATED, MangaListFilter(query = "魔法 & герой"))
        assertEquals("魔法 & герой", context.requests.last().url.queryParameter("q"))
    }

    @Test fun `KdtScans reader decodes quotes brackets and final page without unrelated images`() = runTest {
        val parser = KdtScans(Context { push("1:{\"pages\":[{\"url\":\"https://cdn.test/1.webp?q=][\\\"\"},{\"url\":\"https://cdn.test/final.webp\"}]}\n") })
        val pages = parser.getPages(chapter(parser.source, "/fixture-chapter-1/"))
        assertEquals(2, pages.size)
        assertTrue(pages.first().url.contains("q="))
        assertEquals("https://cdn.test/final.webp", pages.last().url)
    }

    @Test fun `HentaiPaw gallery identity reader link and raw Unicode queries are explicit`() = runTest {
        val context = Context { request -> if (request.url.encodedPath == "/articles/12") """
            <section id='article-details'><h1>Current</h1><div id='button-group'>
            <a href='/viewer?articleId=12&amp;page=1'>Read</a></div></section>
        """ else """
            <main><a href='/articles/rank'>Rank</a><a href='/articles/12'><img alt='Fixture' src='/cover.webp'></a>
            <a href='/articles/12'><img alt='Duplicate'></a><a href='/articles/13'>No image</a></main>
        """ }
        val parser = HentaiPaw(context)
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        assertEquals("/articles/12", entry.url)
        assertEquals(ContentRating.ADULT, entry.contentRating)
        val details = parser.getDetails(entry)
        assertEquals(entry.id, details.id)
        assertEquals("/viewer?articleId=12&page=1", details.chapters!!.single().url)
        parser.getListPage(2, SortOrder.UPDATED, MangaListFilter(query = "魔法 & герой"))
        assertEquals("魔法 & герой", context.requests.last().url.queryParameter("keyword"))
        assertEquals("2", context.requests.last().url.queryParameter("page"))
    }

    @Test fun `HentaiPaw rejects mismatched reader identity and missing blocked payload`() = runTest {
        val parser = HentaiPaw(Context { """<div id='article-details'><div id='button-group'><a href='/viewer?articleId=99'>Read</a></div></div>""" })
        assertInstanceOf(ParseException::class.java,
            runCatching { parser.getDetails(manga(parser.source, "/articles/12")) }.exceptionOrNull())
        assertInstanceOf(ParseException::class.java,
            runCatching { parser.getPages(chapter(parser.source, "/viewer?articleId=12&page=1")) }.exceptionOrNull())
    }

    @Test fun `KappaBeast paginates fractional chapter identities and fetches exact source pages`() = runTest {
        val context = Context { request -> when {
            request.url.encodedPath == "/api/mangas" -> """{"data":[{"slug":"fixture","documentId":"doc","title":"Fixture"}]}"""
            request.url.queryParameter("filters[number][\$eq]") != null ->
                """{"data":[{"htmlContent":"<img src='https://cdn.test/ad.webp'><div class='separator'><a href='https://cdn.test/first.webp?token=a%2Bb'><img src='https://cdn.test/thumb'></a></div><div class='separator'><a href='https://cdn.test/final.webp'>Final</a></div>"}]}"""
            request.url.queryParameter("pagination[page]") == "2" ->
                """{"data":[{"number":"2.50"}],"meta":{"pagination":{"pageCount":2,"total":2}}}"""
            else -> """{"data":[{"number":1}],"meta":{"pagination":{"pageCount":2,"total":2}}}"""
        } }
        val parser = KappaBeast(context)
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        assertNull(entry.contentRating)
        val details = parser.getDetails(entry.copy(id = 987L))
        assertEquals(987L, details.id)
        assertEquals(listOf(1f, 2.5f), details.chapters!!.map { it.number })
        assertEquals("/reader/fixture/2.5#doc", details.chapters!!.last().url)
        val pages = parser.getPages(details.chapters!!.last())
        assertEquals(listOf("https://cdn.test/first.webp?token=a%2Bb", "https://cdn.test/final.webp"), pages.map { it.url })
        assertEquals("2.5", context.requests.last().url.queryParameter("filters[number][\$eq]"))
        parser.getListPage(2, SortOrder.UPDATED, MangaListFilter(query = "魔法 & герой"))
        assertEquals("魔法 & герой", context.requests.last().url.queryParameter("filters[title][\$containsi]"))
    }

    @Test fun `KappaBeast rejects ambiguous repeated chapter identity instead of a partial list`() = runTest {
        val parser = KappaBeast(Context { request -> if (request.url.encodedPath == "/api/mangas")
            """{"data":[{"slug":"fixture","documentId":"doc","title":"Fixture"}]}"""
        else """{"data":[{"number":1}],"meta":{"pagination":{"pageCount":2,"total":2}}}""" })
        assertInstanceOf(ParseException::class.java,
            runCatching { parser.getDetails(manga(parser.source, "/series/fixture#doc")) }.exceptionOrNull())
    }

    @Test fun `KappaBeast bounds decimal expansion before allocating chapter identity`() = runTest {
        for (number in listOf("1e-99999999", "1e99999999", "NaN", "Infinity", "-1", "not a number")) {
            val parser = KappaBeast(Context { request -> if (request.url.encodedPath == "/api/mangas")
                """{"data":[{"slug":"fixture","documentId":"doc","title":"Fixture"}]}"""
            else """{"data":[{"number":"$number"}],"meta":{"pagination":{"pageCount":1,"total":1}}}""" })
            assertInstanceOf(ParseException::class.java,
                runCatching { parser.getDetails(manga(parser.source, "/series/fixture#doc")) }.exceptionOrNull())
        }
    }

    @Test fun `MangaHere resolves full title keeps chapter identity and signed final image`() = runTest {
        val context = Context { request -> when {
            request.url.encodedPath.startsWith("/roll_manga/") -> """
                <img src='https://cdn.test/ad'><div id='viewer'>
                <img src='/loading.gif' data-original='//cdn.test/first.jpg?token=a%2Bb&amp;ttl=3'>
                <img data-original='https://cdn.test/final.jpg'></div>
            """
            request.url.encodedPath == "/manga/fixture/" -> """
                <div class='manga-detail-top'><p class='title'>Full title here</p></div>
                <div class='manga-chapters'><a href='/manga/fixture/c002.5/'>C.2.5</a>
                <a href='/manga/fixture/c001/'>C.1</a></div>
            """
            else -> """<ul class='manga-list'><li><div class='post'><a href='/manga/fixture/'>
                <p class='title'>Full title...</p><img src='/cover.jpg'></a></div></li></ul>"""
        } }
        val parser = MangaHereEn(context)
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        val details = parser.getDetails(entry)
        assertEquals(entry.id, details.id)
        assertEquals("Full title here", details.title)
        assertEquals(listOf(1f, 2.5f), details.chapters!!.map { it.number })
        assertEquals(listOf("https://cdn.test/first.jpg?token=a%2Bb&ttl=3", "https://cdn.test/final.jpg"),
            parser.getPages(details.chapters!!.first()).map { it.url })
        assertEquals("/roll_manga/fixture/c001/", context.requests.last().url.encodedPath)
        val count = context.requests.size
        assertTrue(parser.getListPage(2, SortOrder.UPDATED, MangaListFilter()).isEmpty())
        assertEquals(count, context.requests.size)
        parser.getListPage(1, SortOrder.UPDATED, MangaListFilter(query = "魔法 & герой"))
        assertEquals("魔法 & герой", context.requests.last().url.queryParameter("query"))
    }

    @Test fun `Lava retains locked chapter membership source URLs Arabic search and explicit pages`() = runTest {
        val context = Context { request -> when {
            request.url.encodedPath == "/manga/fixture/" -> """
                <div class='lh-container'><h1 class='lh-title'>Current</h1></div>
                <div id='chapters-list-container'><div class='ch-item locked' data-ch='2.5'>
                <a class='ch-main-anchor' href='/fixture-chapter-2-5/'><span class='ch-num'>فصل 2.5</span></a></div>
                <div class='ch-item' data-ch='1'><a class='ch-main-anchor' href='/fixture-chapter-1/'>فصل 1</a></div></div>
            """
            request.url.encodedPath.endsWith("chapter-1/") -> """
                <img src='https://cdn.test/ad.jpg'><div id='readerarea'>
                <img src='https://cdn.test/first.jpg'><img src='https://cdn.test/final.jpg'></div>
            """
            else -> """<div class='listupd'><div class='manga-card-v'><h3 class='card-v-title'>
                <a href='/manga/fixture/'>Fixture</a></h3></div></div>"""
        } }
        val parser = LavaScans(context)
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        val details = parser.getDetails(entry)
        assertEquals(entry.id, details.id)
        assertNull(details.contentRating)
        assertEquals(listOf(1f, 2.5f), details.chapters!!.map { it.number })
        assertEquals(details.chapters!!.map { parser.generateUid(it.url) }, details.chapters!!.map { it.id })
        assertEquals(listOf("https://cdn.test/first.jpg", "https://cdn.test/final.jpg"),
            parser.getPages(details.chapters!!.first()).map { it.url })
        assertInstanceOf(ParseException::class.java,
            runCatching { parser.getPages(details.chapters!!.last()) }.exceptionOrNull())
        parser.getListPage(2, SortOrder.UPDATED, MangaListFilter(query = "魔法 & عربي"))
        assertEquals("魔法 & عربي", context.requests.last().url.queryParameter("s"))
        assertEquals("/page/2/", context.requests.last().url.encodedPath)
    }

    @Test fun `Kagane all language query is omitted and explicit locale retains exact conversion`() = runTest {
        val context = Context { """{"content":[]}""" }
        val parser = Kagane(context)
        parser.getListPage(1, SortOrder.UPDATED, MangaListFilter())
        fun body() = org.json.JSONObject(okio.Buffer().also { context.requests.last().body!!.writeTo(it) }.readUtf8())
        assertFalse(body().has("content_lang"))
        assertEquals("kagane.si", context.requests.last().url.host)
        parser.getListPage(1, SortOrder.UPDATED, MangaListFilter(locale = java.util.Locale("pt", "BR")))
        assertEquals("pt-BR", body().getJSONArray("content_lang").getString(0))
        assertEquals(1, body().getJSONArray("content_lang").length())
    }

    @Test fun `Kagane cursor pages preserve saved and chapter identity and reject repeated cursor`() = runTest {
        val context = Context { request -> when {
            !request.url.encodedPath.endsWith("/books") -> "{}"
            request.url.queryParameter("before") == null ->
                """{"items":[{"id":"two","chapter_no":"2.5"}],"next_cursor":"opaque/?+"}"""
            else -> """{"items":[{"id":"one","chapter_no":"1"}],"next_cursor":null}"""
        } }
        val parser = Kagane(context)
        val saved = manga(parser.source, "series-id")
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(saved.url, details.url)
        assertEquals(listOf(1f, 2.5f), details.chapters!!.map { it.number })
        assertEquals(parser.generateUid("series-id:two"), details.chapters!!.last().id)
        assertEquals("opaque/?+", context.requests.last().url.queryParameter("before"))
        val repeated = Kagane(Context { request -> if (!request.url.encodedPath.endsWith("/books")) "{}"
            else """{"items":[{"id":"one","chapter_no":"1"}],"next_cursor":"again"}""" })
        assertInstanceOf(ParseException::class.java,
            runCatching { repeated.getDetails(saved) }.exceptionOrNull())
        for (cursor in listOf("123", "{}", "[]", "true")) {
            val malformed = Kagane(Context { request ->
                if (!request.url.encodedPath.endsWith("/books")) "{}"
                else """{"items":[{"id":"one","chapter_no":"1"}],"next_cursor":$cursor}"""
            })
            assertInstanceOf(ParseException::class.java,
                runCatching { malformed.getDetails(saved) }.exceptionOrNull())
        }
    }

    private fun push(data: String) = "<script>self.__next_f.push(${JSONArray().put(1).put(data)});</script>"

    private fun manga(source: MangaSource, url: String) = Manga(
        id = 987L, title = "Saved", altTitles = emptySet(), url = url, publicUrl = "https://fixture.test$url",
        rating = RATING_UNKNOWN, contentRating = null, coverUrl = null, tags = emptySet(), state = null,
        authors = emptySet(), source = source,
    )

    private fun chapter(source: MangaSource, url: String) = MangaChapter(
        id = 1L, title = null, number = 1f, volume = 0, url = url, scanlator = null,
        uploadDate = 0, branch = null, source = source,
    )

    private class Context(private val body: (Request) -> String) : MangaLoaderContext() {
        val requests = ArrayList<Request>()
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            requests += chain.request()
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

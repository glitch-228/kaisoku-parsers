package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.ar.Azoramoon
import org.koitharu.kotatsu.parsers.site.en.SanaScans
import org.koitharu.kotatsu.parsers.site.mangareader.es.LectorReaderPayload
import org.koitharu.kotatsu.parsers.site.mangareader.es.MiauScan
import org.koitharu.kotatsu.parsers.util.generateUid
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class SourceCompletionRepairsTest {

    @Test fun `Azora rendered chapters retain source URLs and skip duplicate or unrelated links`() = runTest {
        val parser = Azoramoon(Context { """
            <a href='/series/fixture/chapter-1'>Start reading</a>
            <a href='/series/fixture/chapter-2.5'>Chapter 2.5</a>
            <a href='/series/fixture/chapter-1'>Chapter 1</a>
            <a href='/series/another/chapter-90'>Other series</a>
            <a href='https://unrelated.test/series/fixture/chapter-9'>External</a>
            <a href='/series/fixture/chapter-2/image'>Not a chapter</a>
            <a href='/series/fixture/chapter-latest'>Not a number</a>
        """ })
        val saved = manga(parser.source)
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(saved.url, details.url)
        val chapters = details.chapters.orEmpty()
        assertEquals(listOf(1f, 2.5f), chapters.map { it.number })
        assertEquals(listOf("/series/fixture/chapter-1", "/series/fixture/chapter-2.5"), chapters.map { it.url })
        assertEquals(chapters.map { parser.generateUid(it.url) }, chapters.map { it.id })
        assertTrue(chapters.all { it.uploadDate == 0L && it.branch == null })
    }

    @Test fun `Azora still accepts the legacy Astro chapter array before rendered links`() = runTest {
        val parser = Azoramoon(Context { """
            <section data-series-tab-panel='chapters'><astro-island props='{"initialChap":[1,[{"slug":[0,"chapter-3"],"number":[0,3],"title":[0,"Saved"],"createdAt":[0,"2026-10-18T00:00:00.000Z"]}]]}'></astro-island></section>
            <a href='/series/fixture/chapter-99'>Different presentation</a>
        """ })
        val chapter = parser.getDetails(manga(parser.source)).chapters!!.single()
        assertEquals("/series/fixture/chapter-3", chapter.url)
        assertEquals(3f, chapter.number)
        assertTrue(chapter.uploadDate > 0)
    }

    @Test fun `Azora keeps the final image and ignores unrelated images`() = runTest {
        val parser = Azoramoon(Context { """
            <img src='https://cdn.test/advert.jpg'>
            <section itemprop='articleBody'><figure><img src='https://cdn.test/1.jpg'></figure><figure><img src='https://cdn.test/2.jpg'></figure></section>
        """ })
        assertEquals(listOf("https://cdn.test/1.jpg", "https://cdn.test/2.jpg"),
            parser.getPages(chapter(parser.source)).map { it.url })
    }

    @Test fun `Lector decrypts the packed public reader and omits only explicit skipped pages`() {
        val urls = listOf("https://cdn.test/first.webp", "https://cdn.test/advert.webp", "https://cdn.test/final.webp")
        val blob = encryptedPayload(urls, listOf(1))
        val dictionary = listOf("ReaderBlob", "start", "k", blob.getString("k"), "iv", blob.getString("iv"),
            "c", blob.getString("c")).joinToString("|")
        val packed = """eval(function(p,a,c,k,e,d){return p}('0.1({"2":"3","4":"5","6":"7"});',10,8,'$dictionary'.split('|'),0,{}))"""
        assertEquals(listOf(urls.first(), urls.last()), LectorReaderPayload.images(packed, "https://reader.test/chapter"))
    }

    @Test fun `Miau moved listings and current chapters retain saved identities`() = runTest {
        val parser = MiauScan(Context { request ->
            if (request.url.encodedPath == "/manga/") """
                <div class='listupd'><div class='bs'><div class='bsx'><a href='/manga/fixture/' title='Fixture'><div class='tt'>Fixture</div><img class='ts-post-image' src='/cover.webp'></a></div></div></div>
            """ else """
                <h1>Fixture</h1><ul class='lm4-chapter-grid'>
                <li><a class='lm4-chapter-link' href='/fixture-capitulo-2-5/' data-chapter-number='2.5' data-chapter-title='Capítulo 2.5'><span class='lm4-chapter-date'>19/10/2026</span></a></li>
                <li><a class='lm4-chapter-link' href='/fixture-capitulo-1/' data-chapter-number='1'></a></li>
                <li><a class='lm4-chapter-link' href='#/chapter-{{number}}' data-chapter-number='3'></a></li></ul>
            """
        })
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        assertEquals("/manga/fixture/", entry.url)
        val saved = entry.copy(id = 123456L, contentRating = ContentRating.ADULT)
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(saved.url, details.url)
        assertEquals(ContentRating.ADULT, details.contentRating)
        assertEquals(listOf(1f, 2.5f), details.chapters!!.map { it.number })
        assertTrue(details.chapters!!.last().uploadDate > 0)
        assertEquals(details.chapters!!.map { parser.generateUid(it.url) }, details.chapters!!.map { it.id })
    }

    @Test fun `Miau reads lazy full images before unrelated UI scripts and keeps the last page`() = runTest {
        val parser = MiauScan(Context { """
            <img src='https://cdn.test/advert.jpg'><div id='readerarea'>
            <img src='data:image/svg+xml,blank' data-src='https://cdn.test/1.webp'>
            <img src='https://cdn.test/final.webp'></div><script>if(window.ts_reader){doSomething();}</script>
        """ })
        assertEquals(listOf("https://cdn.test/1.webp", "https://cdn.test/final.webp"),
            parser.getPages(chapter(parser.source)).map { it.url })
    }

    @Test fun `Sana searches preserve filters encoding and independent page requests`() = runTest {
        val requests = ArrayList<Request>()
        val parser = SanaScans(Context { request -> requests.add(request); """{"posts":[]}""" })
        val tag = MangaTag("Magic", "5", parser.source)
        parser.getListPage(2, SortOrder.UPDATED, MangaListFilter(query = "魔法 & герой", tags = setOf(tag)))
        assertEquals("魔法 & герой", requests.last().url.queryParameter("searchTerm"))
        assertEquals("5", requests.last().url.queryParameter("genreIds"))
        assertEquals("2", requests.last().url.queryParameter("page"))
        parser.getListPage(1, SortOrder.ALPHABETICAL, MangaListFilter())
        assertEquals("1", requests.last().url.queryParameter("page"))
        assertNull(requests.last().url.queryParameter("genreIds"))
        assertEquals("asc", requests.last().url.queryParameter("orderDirection"))
    }

    @Test fun `Sana uses source scoped IDs skips novels and completes chapter pagination`() = runTest {
        val requests = ArrayList<Request>()
        val parser = SanaScans(Context { request ->
            requests.add(request)
            when (request.url.encodedPath) {
                "/api/query" -> """{"posts":[{"id":7,"slug":"fixture","postTitle":"Fixture","seriesType":"MANGA"},{"id":8,"slug":"novel","postTitle":"Text","isNovel":true}]}"""
                "/api/post" -> """{"totalChapterCount":3,"post":{"id":7,"slug":"fixture","postTitle":"Current","chapters":[]}}"""
                else -> if (request.url.queryParameter("skip") == "0") """{"totalChapterCount":3,"post":{"chapters":[{"slug":"chapter-1","number":1},{"slug":"chapter-2.5","number":2.5}]}}"""
                    else """{"totalChapterCount":3,"post":{"chapters":[{"slug":"chapter-3","number":3}]}}"""
            }
        })
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        assertEquals(parser.generateUid("/series/fixture"), entry.id)
        val saved = entry.copy(id = 123456L, contentRating = ContentRating.ADULT)
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(saved.url, details.url)
        assertEquals(ContentRating.ADULT, details.contentRating)
        assertEquals(listOf(1f, 2.5f, 3f), details.chapters!!.map { it.number })
        assertEquals(details.chapters!!.map { parser.generateUid(it.url) }, details.chapters!!.map { it.id })
        assertEquals(listOf("0", "2"), requests.filter { it.url.encodedPath == "/api/chapters" }.map { it.url.queryParameter("skip") })
    }

    @Test fun `Sana refuses repeated incomplete chapter pages and locked reader content`() = runTest {
        val parser = SanaScans(Context { request -> when (request.url.encodedPath) {
            "/api/post" -> """{"totalChapterCount":2,"post":{"id":7,"slug":"fixture","postTitle":"Fixture"}}"""
            "/api/chapters" -> """{"totalChapterCount":2,"post":{"chapters":[{"slug":"chapter-1","number":1}]}}"""
            else -> "<svg class='lucide-lock'></svg><img src='https://cdn.test/cover.webp'>"
        } })
        assertInstanceOf(ParseException::class.java, runCatching { parser.getDetails(manga(parser.source)) }.exceptionOrNull())
        assertInstanceOf(ParseException::class.java, runCatching { parser.getPages(chapter(parser.source)) }.exceptionOrNull())
    }

    @Test fun `Sana reader retains DOM page order and final page without cover or advertisements`() = runTest {
        val parser = SanaScans(Context { """
            <img src='https://cdn.test/cover.webp'>
            <img data-reader-page-image='true' src='https://cdn.test/1.webp'>
            <img data-reader-page-image='true' src='https://cdn.test/final.webp'>
        """ })
        assertEquals(listOf("https://cdn.test/1.webp", "https://cdn.test/final.webp"),
            parser.getPages(chapter(parser.source)).map { it.url })
    }

    @Test fun `Lector legacy reader keeps the last URL and protocol relative identity`() {
        val script = """ts_reader.run({"sources":[{"images":["//cdn.test/first.webp","https://cdn.test/final.webp"]}]});"""
        assertEquals(listOf("https://cdn.test/first.webp", "https://cdn.test/final.webp"),
            LectorReaderPayload.images(script, "https://reader.test/chapter"))
    }

    @Test fun `Lector scans real sized packed ciphertext without recursive regular expressions`() {
        val urls = List(300) { "https://cdn.test/$it/final-page.webp" }
        val blob = encryptedPayload(urls, emptyList())
        val dictionary = listOf("ReaderBlob", "start", "k", blob.getString("k"), "iv", blob.getString("iv"),
            "c", blob.getString("c")).joinToString("|")
        val packed = """eval(function(p,a,c,k,e,d){return p}('0.1({"2":"3","4":"5","6":"7"});',10,8,'$dictionary'.split('|'),0,{}))"""
        assertTrue(packed.length > 10_000)
        assertEquals(urls, LectorReaderPayload.images(packed, "https://reader.test/chapter"))
    }

    @Test fun `Lector rejects bad ciphertext and invalid skip indices without fallback placeholders`() {
        val blob = encryptedPayload(listOf("https://cdn.test/first.webp"), emptyList())
        val encoded = Base64.getDecoder().decode(blob.getString("c"))
        encoded[0] = (encoded[0].toInt() xor 1).toByte()
        blob.put("c", Base64.getEncoder().encodeToString(encoded))
        assertThrows(ParseException::class.java) {
            LectorReaderPayload.images("ReaderBlob.start($blob);", "https://reader.test/chapter")
        }
        val invalidSkip = encryptedPayload(listOf("https://cdn.test/first.webp"), listOf(2))
        assertThrows(ParseException::class.java) {
            LectorReaderPayload.images("ReaderBlob.start($invalidSkip);", "https://reader.test/chapter")
        }
    }

    @Test fun `Lector rejects oversized or executable payloads and fake images`() {
        for (script in listOf(
            "x".repeat(524_289),
            "fetch('https://unrelated.test/'); globalThis.executed=true;",
            """ts_reader.run({"sources":[{"images":["https://lectorhentai.com/assets/img/readerarea.svg"]}]});""",
            """ts_reader.run({"sources":[{"images":["javascript:alert(1)"]}]});""",
        )) {
            assertThrows(ParseException::class.java) {
                LectorReaderPayload.images(script, "https://reader.test/chapter")
            }
        }
    }

    private fun encryptedPayload(urls: List<String>, skip: List<Int>): JSONObject {
        val key = ByteArray(32) { it.toByte() }
        val iv = ByteArray(12) { (it + 10).toByte() }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(JSONObject().put("p", JSONArray(urls)).put("s", JSONArray(skip))
            .toString().toByteArray())
        return JSONObject().put("k", Base64.getEncoder().encodeToString(key))
            .put("iv", Base64.getEncoder().encodeToString(iv)).put("c", Base64.getEncoder().encodeToString(encrypted))
    }

    private fun manga(source: MangaSource) = Manga(
        id = 987654L, title = "Saved", altTitles = emptySet(), url = "/series/fixture",
        publicUrl = "https://azorafly.com/series/fixture", rating = RATING_UNKNOWN,
        contentRating = null, coverUrl = null, tags = emptySet(), state = null, authors = emptySet(), source = source,
    )

    private fun chapter(source: MangaSource) = MangaChapter(
        id = 1, title = null, number = 1f, volume = 0, url = "/series/fixture/chapter-1", scanlator = null,
        uploadDate = 0, branch = null, source = source,
    )

    private class Context(private val body: (Request) -> String) : MangaLoaderContext() {
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
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

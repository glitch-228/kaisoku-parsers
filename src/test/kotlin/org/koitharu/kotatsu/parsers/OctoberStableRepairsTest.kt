package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.ar.Azoramoon
import org.koitharu.kotatsu.parsers.site.en.HeyToonParser
import org.koitharu.kotatsu.parsers.site.id.Voratoon
import org.koitharu.kotatsu.parsers.site.keyoapp.en.TimelessToons
import org.koitharu.kotatsu.parsers.site.madara.all.Manhwa18Cc
import org.koitharu.kotatsu.parsers.site.mangareader.es.MangaTv
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.site.natsu.id.Ikiru
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class OctoberStableRepairsTest {
    @Test fun `Ikiru legacy domain and references keep saved title and chapter identities`() = runTest {
        val ctx = Context("06.ikiru.wtf") { request -> when {
            request.url.encodedPath.contains("/chapter/") -> """{"data":{"medias":[{"filePath":"https://cdn.test/p.webp","pageNumber":1}]}}"""
            else -> """{"data":{"title":"Updated","metadata":{},"chapters":{"chapters":[{"number":"1.5"}]}}}"""
        } }
        val parser = Ikiru(ctx)
        val saved = manga(parser.source, "https://06.ikiru.wtf/manga/fixture/").copy(contentRating = ContentRating.ADULT)
        val details = parser.getDetails(saved)
        assertEquals(saved.id, details.id)
        assertEquals(saved.url, details.url)
        assertEquals(ContentRating.ADULT, details.contentRating)
        val chapter = details.chapters!!.single()
        assertEquals(parser.generateUid("/manga/fixture/chapter-1.5"), chapter.id)
        assertEquals("https://cdn.test/p.webp", parser.getPages(chapter.copy(url = "https://05.ikiru.wtf/manga/fixture/chapter-1.5/")).single().url)
        assertTrue(ctx.requests.all { it.url.host == "09.ikiru.wtf" })
        assertEquals("fixture", ctx.requests.last().url.queryParameter("mangaSlug"))
    }

    @Test fun `Ikiru pagination filters and custom hosts stay independent`() = runTest {
        val ctx = Context("custom.test") { """{"data":{"mangas":[]}}""" }
        val parser = Ikiru(ctx)
        parser.getListPage(3, SortOrder.ALPHABETICAL, MangaListFilter(query = "a & b", types = setOf(ContentType.MANHWA)))
        val url = ctx.requests.single().url
        assertEquals("custom.test", url.host)
        assertEquals("3", url.queryParameter("page"))
        assertEquals("a & b", url.queryParameter("query"))
        assertEquals("title", url.queryParameter("sortBy"))
        assertEquals("asc", url.queryParameter("sort"))
        assertEquals("MANHWA", url.queryParameter("type"))
    }

    @Test fun `Ikiru missing chapters and Voratoon encrypted responses do not erase caches`() = runTest {
        val parser = Ikiru(Context { """{"data":{"title":"Title"}}""" })
        try { parser.getDetails(manga(parser.source, "/manga/fixture")); fail("Missing list must fail") } catch (_: org.json.JSONException) {}
        val vora = Voratoon(Context { """{"encrypted":"v1.future-format"}""" })
        try { vora.getListPage(1, SortOrder.UPDATED, MangaListFilter()); fail("Encryption cannot be an empty catalog") } catch (_: org.json.JSONException) {}
    }

    @Test fun `MangaTV accepts authenticated ReaderBlob without evaluating scripts`() = runTest {
        val key = ByteArray(32) { it.toByte() }; val iv = ByteArray(12) { (it + 1).toByte() }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal("""{"p":["https://cdn.test/1.webp","//cdn.test/2.webp"]}""".toByteArray())
        fun enc(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
        val blob = JSONObject().put("k", enc(key)).put("iv", enc(iv)).put("c", enc(encrypted))
        val parser = MangaTv(Context { "<script>ReaderBlob.start($blob)</script>" })
        val pages = parser.getPages(chapter(parser.source))
        assertEquals(listOf("https://cdn.test/1.webp", "https://cdn.test/2.webp"), pages.map { it.url })
        assertEquals(parser.generateUid(pages.first().url), pages.first().id)
        encrypted[0] = (encrypted[0].toInt() xor 1).toByte()
        blob.put("c", enc(encrypted))
        try { parser.getPages(chapter(parser.source)); fail("Tampering must fail") } catch (_: javax.crypto.AEADBadTagException) {}
    }

    @Test fun `MangaTV keeps plain and base64 legacy image arrays`() = runTest {
        val encoded = Base64.getEncoder().encodeToString("https://cdn.test/old.jpg".toByteArray())
        val parser = MangaTv(Context { """<script>ts_reader.run({"images":["$encoded","https://cdn.test/new.jpg","/readerarea.svg"]})</script>""" })
        assertEquals(listOf("https://cdn.test/old.jpg", "https://cdn.test/new.jpg"), parser.getPages(chapter(parser.source)).map { it.url })
    }

    @Test fun `MangaTV rejects invalid encrypted allocation bounds`() = runTest {
        val blob = JSONObject().put("k", "YQ==").put("iv", "YQ==").put("c", "YQ==")
        val parser = MangaTv(Context { "<script>ReaderBlob.start($blob)</script>" })
        try { parser.getPages(chapter(parser.source)); fail("Invalid key must fail") } catch (_: IllegalArgumentException) {}
    }

    @Test fun `HeyToon retired host is rebased without changing IDs or custom domains`() = runTest {
        for ((configured, expected) in listOf("heytoon.net" to "toonhey.com", "custom.test" to "custom.test")) {
            val ctx = Context(configured) { """[{"link":"/fixture","title":"Fixture","image":"/c.jpg"}]""" }
            val parser = HeyToonParser(ctx)
            // Empty remote search still proves request routing without inventing matches.
            val empty = Context(configured) { "[]" }; val routed = HeyToonParser(empty)
            assertTrue(routed.getListPage(1, SortOrder.UPDATED, MangaListFilter(query = "fixture")).isEmpty())
            assertEquals(expected, empty.requests.single().url.host)
            assertEquals(configured, parser.domain)
        }
    }

    @Test fun `Timeless fallback excludes unrelated non latest cards`() = runTest {
        val ctx = Context { """<div class='grid'><div class='group latest-poster'><a href='/series/keep'><h3>Keep</h3></a></div><div class='group'><a href='/series/no'><h3>Unrelated</h3></a></div></div>""" }
        val parser = TimelessToons(ctx)
        assertEquals(listOf("/series/keep"), parser.getList(SortOrder.UPDATED, MangaListFilter()).map { it.url })
    }

    @Test fun `Manhwa18 native details parse short and long years equally`() = runTest {
        for (date in listOf("24 Oct 26", "24 Oct 2026")) {
            val ctx = Context { """<h1>Fixture</h1><ul class='row-content-chapter'><li class='a-h'><a href='/manga/fixture/ch-1'>Chapter 1</a><span class='chapter-time'>$date</span></li></ul>""" }
            val parser = Manhwa18Cc(ctx)
            val ch = parser.getDetails(manga(parser.source, "/manga/fixture/")).chapters!!.single()
            assertEquals(LocalDate.of(2026, 10, 24).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), ch.uploadDate)
            assertEquals(parser.generateUid("/manga/fixture/ch-1"), ch.id)
        }
    }

    @Test fun `Azora API fetches missing chapters and retains decimal URL identities`() = runTest {
        val ctx = Context { request -> when (request.url.encodedPath) {
            "/api/post" -> """{"totalChapterCount":2,"post":{"id":123,"postTitle":"Updated","chapters":[]}}"""
            "/api/chapters" -> """{"post":{"chapters":[{"slug":"chapter-2.5","number":2.5},{"slug":"chapter-1","number":1}]}}"""
            else -> error("Unexpected request")
        } }
        val parser = Azoramoon(ctx)
        val saved = manga(parser.source, "/series/fixture")
        val result = parser.getDetails(saved)
        assertEquals(saved.id, result.id)
        assertEquals(saved.url, result.url)
        assertEquals(listOf(1f, 2.5f), result.chapters!!.map { it.number })
        assertEquals(result.chapters!!.map { parser.generateUid(it.url) }, result.chapters!!.map { it.id })
        assertEquals(2, ctx.requests.size)
        assertTrue(ctx.requests.all { it.url.host == "api.azorafly.com" })
    }

    @Test fun `Azora API sorts images and does not fall through declared locks`() = runTest {
        var locked = false
        val ctx = Context { if (locked) """{"chapter":{"isPermanentlyLocked":true}}""" else
            """{"chapter":{"images":[{"order":2,"url":"https://cdn.test/last.webp"},{"order":1,"url":"https://cdn.test/first.webp"}]}}""" }
        val parser = Azoramoon(ctx)
        val ch = chapter(parser.source).copy(url = "/series/fixture/chapter-1")
        assertEquals(listOf("https://cdn.test/first.webp", "https://cdn.test/last.webp"), parser.getPages(ch).map { it.url })
        locked = true
        try { parser.getPages(ch); fail("Declared lock must remain an error") }
        catch (error: org.koitharu.kotatsu.parsers.exception.ParseException) { assertTrue(error.message!!.contains("locked")) }
        assertEquals(2, ctx.requests.size)
    }

    private fun manga(source: MangaSource, url: String) = Manga(42, "Saved", emptySet(), url,
        "https://fixture.test", -1f, null, null, emptySet(), null, emptySet(), source = source)
    private fun chapter(source: MangaSource) = MangaChapter(id = 7, url = "/fixture/1", title = "Chapter 1", number = 1f, volume = 0, scanlator = null, uploadDate = 0L, branch = null, source = source)
    private class Context(private val configured: String? = null, private val body: (Request) -> String) : MangaLoaderContext() {
        val requests = CopyOnWriteArrayList<Request>()
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request(); requests += request
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("Fixture")
                .body(body(request).toResponseBody()).build()
        }.build()
        override fun getConfig(source: MangaSource) = object : MangaSourceConfig {
            @Suppress("UNCHECKED_CAST") override fun <T> get(key: ConfigKey<T>): T =
                if (key is ConfigKey.Domain && configured != null) configured as T else key.defaultValue
        }
        override fun getDefaultUserAgent() = "test"
        override suspend fun evaluateJs(script: String): String? = error("No JS execution")
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = error("No JS execution")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response = error("unused")
    }
}

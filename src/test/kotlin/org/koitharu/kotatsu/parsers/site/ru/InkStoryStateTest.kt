package org.koitharu.kotatsu.parsers.site.ru

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.*
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.model.*
import java.util.concurrent.TimeUnit

internal class InkStoryStateTest {
    @Test fun `both sources decode migrated details and encrypted chapter pages`() = runTest {
        for (updates in listOf(false, true)) {
            val context = Context()
            val parser = if (updates) MangaOvhUpdatesParser(context) else MangaOVHParser(context)
            val seed = Manga(id = 17, title = "Saved title", altTitles = emptySet(), url = "/content/chainsaw_man-p2",
                publicUrl = "https://inkstory.net/content/chainsaw_man-p2", rating = RATING_UNKNOWN,
                contentRating = null, coverUrl = null, tags = emptySet(), state = null, authors = emptySet(), source = parser.source)
            val details = parser.getDetails(seed)
            assertEquals(seed.id, details.id)
            assertEquals("Продолжение первой части", details.description)
            assertEquals(141, details.chapters!!.size)
            assertNotNull(details.coverUrl)
            val chapter = details.chapters!!.first { it.url.endsWith("a23602d4-a643-46e3-afed-e723c65f434e") }
            val pages = parser.getPages(chapter)
            assertEquals(21, pages.size)
            assertTrue(pages.first().url.contains("#ik=xor:"))
            assertTrue(pages.first().url.contains("width=1600"))
            assertEquals(listOf("details", "chapters", "chapter"), context.requests)
        }
    }

    @Test fun `missing poster does not prevent details or chapters`() = runTest {
        val context = Context(noPoster = true)
        val parser = MangaOVHParser(context)
        val seed = Manga(id = 17, title = "Saved title", altTitles = emptySet(), url = "/content/chainsaw_man-p2",
            publicUrl = "https://inkstory.net/content/chainsaw_man-p2", rating = RATING_UNKNOWN,
            contentRating = null, coverUrl = null, tags = emptySet(), state = null, authors = emptySet(), source = parser.source)
        val details = parser.getDetails(seed)
        assertNull(details.coverUrl)
        assertEquals(141, details.chapters!!.size)
        assertEquals("Продолжение первой части", details.description)
    }

    private class Context(val noPoster: Boolean = false) : MangaLoaderContext() {
        val requests = ArrayList<String>()
        override val cookieJar = CookieJar.NO_COOKIES
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val path = chain.request().url.encodedPath
            val fixture = when {
                path.endsWith("/chapters") -> "chapters"
                path.endsWith("a23602d4-a643-46e3-afed-e723c65f434e") -> "chapter"
                else -> "details"
            }
            requests.add(fixture)
            var html = checkNotNull(javaClass.getResourceAsStream("/inkstory/$fixture.html")).bufferedReader().use { it.readText() }
            if (noPoster) html = html.replace(Regex("poster:\"[^\"]*\""), "poster:null")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(html.toResponseBody("text/html".toMediaType())).build()
        }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "test"
        override suspend fun evaluateJs(script: String): String? = evaluateJs("", script, 5000)
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? {
            val process = ProcessBuilder("node", "-e", "const vm=require('vm'),fs=require('fs');try{process.stdout.write(vm.runInNewContext(fs.readFileSync(0,'utf8'),{},{timeout:5000}));}catch(e){process.stderr.write(e.message);process.exit(1)}").start()
            process.outputStream.bufferedWriter().use { it.write(script) }
            val result = process.inputStream.bufferedReader().readText()
            check(process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0) { process.errorStream.bufferedReader().readText() }
            return if (noPoster) org.json.JSONObject.quote(result) else result
        }
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap) = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

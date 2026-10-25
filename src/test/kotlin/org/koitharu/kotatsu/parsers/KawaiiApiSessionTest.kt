package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.ar.KawiiManga
import org.koitharu.kotatsu.parsers.site.ar.KawaiiTokenStore
import org.koitharu.kotatsu.parsers.util.generateUid
import java.util.concurrent.atomic.AtomicInteger

internal class KawaiiApiSessionTest {
    @Test fun `public token authenticates browse details and pages without changing identities`() = runTest {
        val requests = mutableListOf<Request>()
        val parser = KawiiManga(Context { request ->
            requests.add(request)
            when {
                request.url.encodedPath.endsWith("/token") -> {
                    assertNull(request.header("x-app-token")); 200 to """{"token":"anonymous-session","expiresIn":1800}"""
                }
                else -> {
                    assertEquals("anonymous-session", request.header("x-app-token"))
                    val manga = """{"slug":"fixture","title":"Fixture","chapters":[{"id":"opaque-9","number":1,"title":"First"}]}"""
                    200 to when (request.url.queryParameter("action")) {
                        "browse", "search" -> """{"results":[$manga]}"""
                        "series" -> manga
                        else -> """{"pages":["https://cdn.test/first.webp","https://cdn.test/final.webp"]}"""
                    }
                }
            }
        })
        val entry = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).single()
        val detail = parser.getDetails(entry)
        assertEquals(entry.id, detail.id)
        assertEquals("fixture", detail.url)
        val chapter = detail.chapters!!.single()
        val pages = parser.getPages(chapter)
        assertEquals(listOf("https://cdn.test/first.webp", "https://cdn.test/final.webp"), pages.map { it.url })
        assertEquals(pages.map { parser.generateUid(it.url) }, pages.map { it.id })
        assertEquals(1, requests.count { it.url.encodedPath.endsWith("/token") })
        assertNull(parser.getRequestHeaders()["x-app-token"])
    }

    @Test fun `401 refreshes once and a second rejection is reported without a loop`() = runTest {
        val tokenCount = AtomicInteger(); val reads = AtomicInteger()
        val parser = KawiiManga(Context { request ->
            if (request.url.encodedPath.endsWith("/token")) 200 to """{"token":"token-${tokenCount.incrementAndGet()}","expiresIn":1800}"""
            else { reads.incrementAndGet(); 401 to "{}" }
        })
        assertThrows(AuthRequiredException::class.java) { runBlocking { parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()) } }
        assertEquals(2, tokenCount.get()); assertEquals(2, reads.get())
    }

    @Test fun `expired token is refreshed and a successful retry keeps the result`() = runTest {
        val tokenCount = AtomicInteger(); val reads = AtomicInteger()
        val parser = KawiiManga(Context { request ->
            if (request.url.encodedPath.endsWith("/token")) 200 to """{"token":"token-${tokenCount.incrementAndGet()}","expiresIn":1800}"""
            else if (reads.incrementAndGet() == 1) 401 to "{}" else 200 to """{"results":[]}"""
        })
        assertTrue(parser.getListPage(1, SortOrder.UPDATED, MangaListFilter()).isEmpty())
        assertEquals(2, tokenCount.get()); assertEquals(2, reads.get())
    }

    @Test fun `concurrent requests share one token and short TTL is never extended`() = runTest {
        var now = 100L; val store = KawaiiTokenStore { now }; val loads = AtomicInteger()
        suspend fun token() = store.get { delay(10); "token-${loads.incrementAndGet()}" to 1L }
        assertEquals(setOf("token-1"), (1..8).map { async { token() } }.awaitAll().toSet())
        assertEquals(1, loads.get())
        now = 1_000L
        assertEquals("token-2", token())
        store.invalidate("token-1")
        assertEquals("token-2", token())
        assertEquals(2, loads.get())
    }

    @Test fun `cancellation never publishes a token or blocks later loads`() = runTest {
        val store = KawaiiTokenStore { 0 }; val entered = CompletableDeferred<Unit>()
        val job = launch { store.get { entered.complete(Unit); awaitCancellation() } }
        entered.await(); job.cancelAndJoin()
        assertEquals("fresh", store.get { "fresh" to 30L })
    }

    @Test fun `zero TTL is not cached and maximum lifetime is bounded`() = runTest {
        var now = 0L; val store = KawaiiTokenStore { now }
        assertEquals("zero", store.get { "zero" to 0L })
        assertEquals("long", store.get { "long" to Long.MAX_VALUE })
        now = 86_400_000L
        assertEquals("new", store.get { "new" to 1L })
    }

    private class Context(private val response: (Request) -> Pair<Int, String>) : MangaLoaderContext() {
        override val cookieJar = InMemoryCookieJar()
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val (code, body) = response(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("fixture").body(body.toResponseBody()).build()
        }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "test"
        override suspend fun evaluateJs(script: String): String? = null
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

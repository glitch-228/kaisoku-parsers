package org.koitharu.kotatsu.parsers.site.en

import kotlinx.coroutines.test.runTest
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaLoaderContextMock
import org.koitharu.kotatsu.parsers.SourceConfigMock
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.test_util.mangaOf

internal class MangagoChapterBranchesTest {

	@Test
	fun `keeps the established list while named branches fill their gaps`() = runTest {
		val parser = MangagoParser(MangagoContext())
		val details = parser.getDetails(
			mangaOf(MangaParserSource.MANGAGO, "https://www.mangago.me/read/test"),
		)
		val chapters = checkNotNull(details.chapters)
		val primary = chapters.filter { it.branch == null }
		val groupA = chapters.filter { it.branch == "Group A" }

		assertEquals(listOf("https://www.mangago.me/b1", "https://www.mangago.me/b2", "https://www.mangago.me/b3"), primary.map { it.url })
		assertEquals(listOf("https://www.mangago.me/a1", "https://www.mangago.me/b2", "https://www.mangago.me/a3"), groupA.map { it.url })
		assertNotEquals(primary[1].id, groupA[1].id)
	}

	private class MangagoContext : MangaLoaderContext() {

		override val cookieJar: CookieJar = CookieJar.NO_COOKIES

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor(Interceptor { chain ->
				Response.Builder()
					.request(chain.request())
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(HTML.toResponseBody("text/html".toMediaType()))
					.build()
			})
			.build()

		override fun getConfig(source: MangaSource): MangaSourceConfig = SourceConfigMock()

		override fun getDefaultUserAgent(): String = "test-agent"

		@Deprecated("Provide a base url")
		override suspend fun evaluateJs(script: String): String? = error("Unexpected JavaScript evaluation")

		override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? =
			error("Unexpected JavaScript evaluation")

		override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response =
			MangaLoaderContextMock.redrawImageResponse(response, redraw)

		override fun createBitmap(width: Int, height: Int): Bitmap =
			MangaLoaderContextMock.createBitmap(width, height)
	}

	private companion object {
		const val HTML = """
			<html><body>
			<div class="w-title"><h1>Test</h1></div>
			<div id="information"><img src="/cover.jpg" /></div>
			<table id="chapter_table"><tbody>
			<tr><td><a class="chico" href="/b3">Chapter 3</a></td><td class="no"><a>Group B</a></td><td>1 Jan 2026</td></tr>
			<tr><td><a class="chico" href="/a3">Chapter 3</a></td><td class="no"><a>Group A</a></td><td>1 Jan 2026</td></tr>
			<tr><td><a class="chico" href="/b2">Chapter 2</a></td><td class="no"><a>Group B</a></td><td>1 Jan 2026</td></tr>
			<tr><td><a class="chico" href="/b1">Chapter 1</a></td><td class="no"><a>Group B</a></td><td>1 Jan 2026</td></tr>
			<tr><td><a class="chico" href="/a1">Chapter 1</a></td><td class="no"><a>Group A</a></td><td>1 Jan 2026</td></tr>
			</tbody></table>
			</body></html>
		"""
	}
}

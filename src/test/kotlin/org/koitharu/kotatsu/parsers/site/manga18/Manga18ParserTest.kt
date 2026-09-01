package org.koitharu.kotatsu.parsers.site.manga18

import kotlinx.coroutines.test.runTest
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaLoaderContextMock
import org.koitharu.kotatsu.parsers.SourceConfigMock
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.site.manga18.en.Manga18

internal class Manga18ParserTest {

	@Test
	fun `builds list search and tag routes without duplicate query separators`() = runTest {
		val context = RecordingContext()
		val parser = Manga18(context)

		parser.getList(0, SortOrder.UPDATED, MangaListFilter.EMPTY)
		parser.getList(0, SortOrder.UPDATED, MangaListFilter(query = "office love"))
		parser.getList(
			0,
			SortOrder.POPULARITY,
			MangaListFilter(tags = setOf(MangaTag("Action", "action", MangaParserSource.MANGA18))),
		)

		assertEquals("/list-manga/1", context.requests[0].url.encodedPath)
		assertEquals("latest", context.requests[0].url.queryParameter("order_by"))

		assertEquals("/list-manga/1", context.requests[1].url.encodedPath)
		assertEquals("office love", context.requests[1].url.queryParameter("search"))
		assertEquals("latest", context.requests[1].url.queryParameter("order_by"))
		assertEquals(2, context.requests[1].url.querySize)

		assertEquals("/manga-list/action/1", context.requests[2].url.encodedPath)
		assertEquals("views", context.requests[2].url.queryParameter("order_by"))
	}

	private class RecordingContext : MangaLoaderContext() {

		val requests = mutableListOf<Request>()

		override val cookieJar: CookieJar = CookieJar.NO_COOKIES

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor(Interceptor { chain ->
				requests += chain.request()
				Response.Builder()
					.request(chain.request())
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body("<html><body></body></html>".toResponseBody("text/html".toMediaType()))
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
}

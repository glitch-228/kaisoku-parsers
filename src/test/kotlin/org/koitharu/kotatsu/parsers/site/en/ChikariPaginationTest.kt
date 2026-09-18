package org.koitharu.kotatsu.parsers.site.en

import kotlinx.coroutines.test.runTest
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaLoaderContextMock
import org.koitharu.kotatsu.parsers.SourceConfigMock
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.test_util.mangaOf
import java.util.Collections

internal class ChikariPaginationTest {

	@Test
	fun `fetches every server-capped chapter page`() = runTest {
		val context = ChikariContext()
		val parser = Chikari(context)
		val details = parser.getDetails(
			mangaOf(MangaParserSource.CHIKARI, "https://chikari.moe/series/test-series"),
		)

		val chapters = checkNotNull(details.chapters)
		assertEquals(401, chapters.size)
		assertEquals(1f, chapters.first().number)
		assertEquals(401f, chapters.last().number)
		assertEquals(setOf(0, 200, 400), context.chapterOffsets)
	}

	private class ChikariContext : MangaLoaderContext() {

		val chapterOffsets = Collections.synchronizedSet(LinkedHashSet<Int>())

		override val cookieJar: CookieJar = CookieJar.NO_COOKIES

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor(Interceptor { chain ->
				val request = chain.request()
				val body = when (request.url.encodedPath) {
					"/api/series/test-series" -> """{"data":{"title":"Test series"}}"""
					"/api/series/test-series/chapters" -> chapterPage(request.url.queryParameter("offset")?.toInt())
					else -> error("Unexpected request: ${request.url}")
				}
				Response.Builder()
					.request(request)
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(body.toResponseBody("application/json".toMediaType()))
					.build()
			})
			.build()

		private fun chapterPage(offset: Int?): String {
			val resolvedOffset = requireNotNull(offset)
			chapterOffsets += resolvedOffset
			val items = JSONArray()
			for (number in resolvedOffset + 1..minOf(resolvedOffset + 200, 401)) {
				items.put(JSONObject().put("number", number).put("title", "Chapter $number"))
			}
			return JSONObject().put("total", 401).put("items", items).toString()
		}

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

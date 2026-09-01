package org.koitharu.kotatsu.parsers.site.en

import kotlinx.coroutines.test.runTest
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaLoaderContextMock
import org.koitharu.kotatsu.parsers.SourceConfigMock
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.Locale

internal class KaganeTest {

	@Test
	fun `maps prerelease filter options into Kagane search request`() = runTest {
		val context = KaganeContext()
		val parser = Kagane(context)
		val options = parser.getFilterOptions()

		assertEquals(setOf("Action"), options.availableTags.mapTo(mutableSetOf()) { it.title })
		assertTrue(MangaState.ABANDONED in options.availableStates)
		assertTrue(ContentType.MANHWA in options.availableContentTypes)
		assertTrue(Locale("pt", "br") in options.availableLocales)

		val include = MangaTag("Action", INCLUDED_GENRE, MangaParserSource.KAGANE)
		val exclude = MangaTag("Adult", EXCLUDED_GENRE, MangaParserSource.KAGANE)
		val manga = parser.getList(
			0,
			SortOrder.POPULARITY,
			MangaListFilter(
				tags = setOf(include),
				tagsExclude = setOf(exclude),
				locale = Locale("pt", "br"),
				states = setOf(MangaState.ONGOING),
				contentRating = setOf(ContentRating.ADULT),
				types = setOf(ContentType.MANHWA),
			),
		).single()

		assertEquals("test-series", manga.url)
		assertEquals("Series [Test Source]", manga.title)
		assertEquals("total_views,desc", context.searchUrl?.queryParameter("sort"))

		val body = checkNotNull(context.searchBody)
		assertEquals("pt-BR", body.getJSONArray("content_lang").getString(0))
		assertEquals("Ongoing", body.getJSONArray("upload_status").getString(0))
		assertEquals("Manhwa", body.getJSONArray("format").getString(0))
		assertEquals(setOf("Erotica", "Pornographic"), body.getJSONArray("content_rating").toStringSet())
		assertEquals(INCLUDED_GENRE, body.getJSONObject("genres").getJSONArray("values").getString(0))
		assertEquals(EXCLUDED_GENRE, body.getJSONObject("genres").getJSONArray("exclude").getString(0))
	}

	private class KaganeContext : MangaLoaderContext() {

		var searchUrl: okhttp3.HttpUrl? = null
		var searchBody: JSONObject? = null

		override val cookieJar: CookieJar = CookieJar.NO_COOKIES

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor(Interceptor { chain ->
				val request = chain.request()
				val responseBody = when (request.url.encodedPath) {
					"/api/v2/genres/list" ->
						"""[{"genre_id":"$INCLUDED_GENRE","genre_name":"Action"}]"""

					"/api/v2/search/series" -> {
						searchUrl = request.url
						searchBody = JSONObject(Buffer().also { request.body?.writeTo(it) }.readUtf8())
						"""{"content":[{"id":"test-series","name":"Series","source":"Test Source","content_rating":"Safe"}]}"""
					}

					else -> error("Unexpected request: ${request.url}")
				}
				Response.Builder()
					.request(request)
					.protocol(Protocol.HTTP_1_1)
					.code(200)
					.message("OK")
					.body(responseBody.toResponseBody("application/json".toMediaType()))
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
		private const val INCLUDED_GENRE = "11111111-1111-1111-1111-111111111111"
		private const val EXCLUDED_GENRE = "22222222-2222-2222-2222-222222222222"

		private fun org.json.JSONArray.toStringSet(): Set<String> =
			(0 until length()).mapTo(mutableSetOf()) { getString(it) }
	}
}

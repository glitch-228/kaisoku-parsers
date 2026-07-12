package org.koitharu.kotatsu.parsers.site.ru

import kotlinx.coroutines.test.runTest
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
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
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder

internal class AstraMangaTest {

	@Test
	fun `maps astra api list details chapters and pages`() = runTest {
		val context = AstraContext()
		val parser = AstraManga(context)
		val filter = MangaListFilter(
			query = "test title",
			tags = setOf(MangaTag("Action", "action", parser.source)),
			states = setOf(MangaState.ONGOING),
		)

		val manga = parser.getList(0, SortOrder.NEWEST, filter).single()

		assertEquals("Demo Astra Title", manga.title)
		assertEquals("/manga/demo-astra-title", manga.url)
		assertEquals("https://astramanga.org/manga/demo-astra-title", manga.publicUrl)

		val details = parser.getDetails(manga)
		assertEquals("Detailed synopsis", details.description)
		assertEquals(MangaState.ONGOING, details.state)
		assertEquals(setOf("Action", "Drama"), details.tags.mapTo(mutableSetOf()) { it.title })

		val chapters = checkNotNull(details.chapters)
		assertEquals(listOf(1f, 2f), chapters.map { it.number })
		assertEquals(1767345027105L, chapters[1].uploadDate)
		assertEquals("Глава 2.0", chapters[1].title)
		assertEquals("https://api.astramanga.org/api/v1/chapters/302/pages", chapters[1].url)

		val pages = parser.getPages(chapters[1])
		assertEquals(
			listOf(
				"https://cdn.astramanga.org/pages/302/001.webp",
				"https://cdn.astramanga.org/pages/302/002.webp",
			),
			pages.map { it.url },
		)

		assertEquals(
			listOf(
				"https://api.astramanga.org/api/v1/search?page=1&size=30&query=test%20title&sort=-created_at&status=ongoing&genres=action",
				"https://api.astramanga.org/api/v1/titles/demo-astra-title",
				"https://api.astramanga.org/api/v1/titles/101/branches",
				"https://api.astramanga.org/api/v1/branches/201/chapters",
				"https://api.astramanga.org/api/v1/chapters/302/pages",
			),
			context.requestedUrls,
		)
	}

	private class AstraContext : MangaLoaderContext() {

		val requestedUrls = mutableListOf<String>()

		override val cookieJar: CookieJar = CookieJar.NO_COOKIES

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor(AstraInterceptor(requestedUrls))
			.build()

		override fun getConfig(source: MangaSource): MangaSourceConfig = SourceConfigMock()

		override fun getDefaultUserAgent(): String = "test-agent"

		@Deprecated("Provide a base url")
		override suspend fun evaluateJs(script: String): String? = null

		override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null

		override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response {
			return MangaLoaderContextMock.redrawImageResponse(response, redraw)
		}

		override fun createBitmap(width: Int, height: Int): Bitmap {
			return MangaLoaderContextMock.createBitmap(width, height)
		}
	}

	private class AstraInterceptor(
		private val requestedUrls: MutableList<String>,
	) : Interceptor {

		override fun intercept(chain: Interceptor.Chain): Response {
			val request = chain.request()
			val url = request.url
			requestedUrls += url.toString()
			val body = when (url.encodedPath) {
				"/api/v1/search" -> {
					assertEquals("1", url.queryParameter("page"))
					assertEquals("30", url.queryParameter("size"))
					assertEquals("test title", url.queryParameter("query"))
					assertEquals("-created_at", url.queryParameter("sort"))
					assertEquals("ongoing", url.queryParameter("status"))
					assertEquals("action", url.queryParameter("genres"))
					LIST_JSON
				}
				"/api/v1/titles/demo-astra-title" -> DETAILS_JSON
				"/api/v1/titles/101/branches" -> BRANCHES_JSON
				"/api/v1/branches/201/chapters" -> CHAPTERS_JSON
				"/api/v1/chapters/302/pages" -> PAGES_JSON
				else -> error("Unexpected request: $url")
			}
			return Response.Builder()
				.request(request)
				.protocol(Protocol.HTTP_1_1)
				.code(200)
				.message("OK")
				.body(body.toResponseBody("application/json".toMediaType()))
				.build()
		}
	}

	private companion object {

		private const val LIST_JSON = """
			{
				"data": {
					"titles": [
						{
							"id": 101,
							"slug": "demo-astra-title",
							"name": "Demo Astra Title"
						}
					]
				}
			}
		"""

		private const val DETAILS_JSON = """
			{
				"data": {
					"id": 101,
					"slug": "demo-astra-title",
					"name": "Demo Astra Title",
					"description": "Detailed synopsis",
					"status": "ongoing",
					"genres": [
						{"name": "Action", "slug": "action"},
						{"name": "Drama", "slug": "drama"}
					]
				}
			}
		"""

		private const val BRANCHES_JSON = """
			{
				"data": {
					"branches": [
						{"id": 200, "is_main": false},
						{"id": 201, "is_main": true}
					]
				}
			}
		"""

		private const val CHAPTERS_JSON = """
			{
				"data": {
					"items": [
						{"id": 301, "number": 1.0, "published_at": "2026-01-01T12:00:00"},
						{"id": 302, "number": 2.0, "published_at": "2026-01-02T12:10:27.105103+03:00"}
					]
				}
			}
		"""

		private const val PAGES_JSON = """
			{
				"data": {
					"pages": [
						{"image_url": "https://cdn.astramanga.org/pages/302/001.webp"},
						{"image_url": "https://cdn.astramanga.org/pages/302/002.webp"}
					]
				}
			}
		"""
	}
}

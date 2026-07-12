package org.koitharu.kotatsu.parsers.site.all

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
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder

internal class MangaFireParserTest {

	@Test
	fun `parses current api list details chapters and pages`() = runTest {
		val context = MangaFireContext()
		val parser = MangaFireParser.English(context)
		val filter = MangaListFilter(
			query = "demo title",
			tags = setOf(MangaTag("Action", "1", parser.source)),
			states = setOf(MangaState.ONGOING),
		)

		val manga = parser.getList(0, SortOrder.UPDATED, filter).single()

		assertEquals("Demo MangaFire Title", manga.title)
		assertEquals("/title/abc12-demo-mangafire-title", manga.url)
		assertEquals("https://mangafire.to/title/abc12-demo-mangafire-title", manga.publicUrl)
		assertEquals("https://img.example/poster-large.jpg", manga.coverUrl)
		assertEquals(MangaState.ONGOING, manga.state)

		val details = parser.getDetails(manga)
		assertEquals("Detailed synopsis", details.description)
		assertEquals(setOf("Demo Alt"), details.altTitles)
		assertEquals(setOf("Writer", "Artist"), details.authors)
		assertEquals(setOf("Action", "Ecchi"), details.tags.mapTo(mutableSetOf()) { it.title })
		assertEquals(ContentRating.SUGGESTIVE, details.contentRating)
		assertEquals(4.5f, details.rating)

		val chapters = checkNotNull(details.chapters)
		assertEquals(listOf(1f, 2f, 3f), chapters.map { it.number })
		assertEquals(listOf("Unofficial", "Official", "Volume"), chapters.map { it.branch })
		assertEquals("Chapter 2: Finale", chapters[1].title)
		assertEquals("/title/abc12-demo-mangafire-title/9002", chapters[1].url)
		assertEquals(1783382528000L, chapters[0].uploadDate)
		assertEquals("Volume 3: Collected", chapters[2].title)
		assertEquals("6 chapters", chapters[2].scanlator)

		val pages = parser.getPages(chapters[1])
		assertEquals(
			listOf(
				"https://img.example/page-1.jpg",
				"https://img.example/page-2.jpg",
			),
			pages.map { it.url },
		)
		val volumePages = parser.getPages(chapters[2])
		assertEquals(listOf("https://img.example/volume-page.jpg"), volumePages.map { it.url })

		assertEquals(
			listOf(
				"https://mangafire.to/api/titles?page=1&limit=30&language%5B%5D=en&keyword=demo%20title&genres_in%5B%5D=1&statuses%5B%5D=releasing&order%5Bchapter_updated_at%5D=desc",
				"https://mangafire.to/api/titles/abc12",
				"https://mangafire.to/api/titles/abc12/chapters?page=1&limit=500",
				"https://mangafire.to/api/titles/abc12/volumes?language=en",
				"https://mangafire.to/api/chapters/9002",
				"https://mangafire.to/api/volumes/7003",
			),
			context.requestedUrls,
		)
	}

	private class MangaFireContext : MangaLoaderContext() {

		val requestedUrls = mutableListOf<String>()

		override val cookieJar: CookieJar = CookieJar.NO_COOKIES

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor(MangaFireInterceptor(requestedUrls))
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

	private class MangaFireInterceptor(
		private val requestedUrls: MutableList<String>,
	) : Interceptor {

		override fun intercept(chain: Interceptor.Chain): Response {
			val request = chain.request()
			val url = request.url
			requestedUrls += url.toString()
			val body = when (url.encodedPath) {
				"/api/titles" -> {
					assertEquals("1", url.queryParameter("page"))
					assertEquals("30", url.queryParameter("limit"))
					assertEquals("en", url.queryParameter("language[]"))
					assertEquals("demo title", url.queryParameter("keyword"))
					assertEquals("1", url.queryParameter("genres_in[]"))
					assertEquals("releasing", url.queryParameter("statuses[]"))
					assertEquals("desc", url.queryParameter("order[chapter_updated_at]"))
					LIST_JSON
				}
				"/api/titles/abc12" -> DETAILS_JSON
				"/api/titles/abc12/chapters" -> {
					assertEquals("1", url.queryParameter("page"))
					assertEquals("500", url.queryParameter("limit"))
					CHAPTERS_JSON
				}
				"/api/titles/abc12/volumes" -> VOLUMES_JSON
				"/api/chapters/9002" -> PAGES_JSON
				"/api/volumes/7003" -> VOLUME_PAGES_JSON
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
				"items": [
					{
						"id": 100,
						"hid": "abc12",
						"slug": "demo-mangafire-title",
						"title": "Demo MangaFire Title",
						"status": "releasing",
						"poster": {
							"large": "https://img.example/poster-large.jpg"
						},
						"url": "/title/abc12-demo-mangafire-title"
					}
				],
				"meta": {"lastPage": 1}
			}
		"""

		private const val DETAILS_JSON = """
			{
				"data": {
					"id": 100,
					"hid": "abc12",
					"slug": "demo-mangafire-title",
					"title": "Demo MangaFire Title",
					"status": "releasing",
					"poster": {
						"large": "https://img.example/poster-large.jpg"
					},
					"synopsisHtml": "<p>Detailed synopsis</p>",
					"altTitles": ["Demo Alt"],
					"rating": 9,
					"hasVolumes": true,
					"genres": [
						{"id": 1, "title": "Action"},
						{"id": 7, "title": "Ecchi"}
					],
					"authors": [{"id": 1, "title": "Writer"}],
					"artists": [{"id": 2, "title": "Artist"}],
					"url": "/title/abc12-demo-mangafire-title"
				}
			}
		"""

		private const val CHAPTERS_JSON = """
			{
				"items": [
					{"id": 9002, "number": 2, "name": "Finale", "language": "en", "type": "official", "createdAt": 1783382529},
					{"id": 9001, "number": 1, "name": "", "language": "en", "type": "unofficial", "createdAt": 1783382528}
				],
				"meta": {"lastPage": 1}
			}
		"""

		private const val PAGES_JSON = """
			{
				"data": {
					"pages": [
						{"url": "https://img.example/page-1.jpg"},
						{"url": "https://img.example/page-2.jpg"}
					]
				}
			}
		"""

		private const val VOLUMES_JSON = """
			{
				"items": [
					{"id": 7003, "number": 3, "name": "Collected", "language": "en", "chapterCount": 6}
				]
			}
		"""

		private const val VOLUME_PAGES_JSON = """
			{
				"data": {
					"pages": [{"url": "https://img.example/volume-page.jpg"}]
				}
			}
		"""
	}
}

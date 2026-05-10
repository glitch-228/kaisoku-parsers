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
import org.junit.jupiter.api.Assertions.assertTrue
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
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.json.JSONObject

internal class ComixParserTest {

	@Test
	fun `parses current v1 api manga chapters and pages`() = runTest {
		val context = ComixContext()
		val parser = Comix(context)

		val manga = parser.getList(0, SortOrder.UPDATED, MangaListFilter.EMPTY).single()

		assertEquals("Limitless Ascension: Strength Amid Adversity", manga.title)
		assertEquals("/title/8d9ye-limitless-ascension-strength-amid-adversity", manga.url)
		assertEquals("https://www.comix.to/title/8d9ye-limitless-ascension-strength-amid-adversity", manga.publicUrl)
		assertEquals("https://img.example/poster-large.webp", manga.coverUrl)
		assertEquals(0.84f, manga.rating)
		assertEquals(ContentRating.SAFE, manga.contentRating)
		assertEquals(MangaState.ONGOING, manga.state)

		val details = parser.getDetails(manga)
		assertEquals(setOf("Boundless Climb"), details.altTitles)
		assertEquals("Detailed synopsis", details.description)
		assertEquals(setOf("Action", "Fantasy"), details.tags.mapTo(mutableSetOf()) { it.title })
		assertEquals(setOf("Jane Writer", "John Artist"), details.authors)

		val chapters = checkNotNull(details.chapters)
		assertEquals(listOf(122f, 123f), chapters.map { it.number })
		assertEquals("Drake Scans", chapters.last().scanlator)
		assertEquals("/title/8d9ye-limitless-ascension-strength-amid-adversity/9074688-chapter-123", chapters.last().url)

		val pages = parser.getPages(chapters.last())
		assertEquals(
			listOf(
				"https://img.example/pages/01.webp",
				"https://img.example/pages/02.webp",
			),
			pages.map { it.url },
		)

		assertTrue(context.requestedUrls.any { it.contains("/api/v1/manga?") })
		assertTrue(context.requestedUrls.any { it.contains("/api/v1/manga/8d9ye/chapters") && it.contains("_=test-token") })
		assertTrue(context.requestedUrls.any { it.contains("/api/v1/chapters/9074688") && it.contains("_=test-token") })
	}

	private class ComixContext : MangaLoaderContext() {

		val requestedUrls = mutableListOf<String>()

		override val cookieJar: CookieJar = CookieJar.NO_COOKIES

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor(ComixInterceptor(requestedUrls))
			.build()

		@Deprecated("Provide a base url")
		override suspend fun evaluateJs(script: String): String? = evaluateJs("", script, 10_000L)

		override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? {
			return when {
				script.contains("__COMIX_SIGN__") -> JSONObject.quote("test-token")
				script.contains("chapters-fixture") -> JSONObject.quote(CHAPTERS_JSON)
				script.contains("pages-fixture") -> JSONObject.quote(PAGES_JSON)
				else -> error("Unexpected JS evaluation: ${script.take(120)}")
			}
		}

		override fun getConfig(source: MangaSource): MangaSourceConfig = SourceConfigMock()

		override fun getDefaultUserAgent(): String = "test-agent"

		override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response {
			return MangaLoaderContextMock.redrawImageResponse(response, redraw)
		}

		override fun createBitmap(width: Int, height: Int): Bitmap {
			return MangaLoaderContextMock.createBitmap(width, height)
		}
	}

	private class ComixInterceptor(
		private val requestedUrls: MutableList<String>,
	) : Interceptor {

		override fun intercept(chain: Interceptor.Chain): Response {
			val request = chain.request()
			val url = request.url
			requestedUrls += url.toString()
			val body = when (url.encodedPath) {
				"/home" -> HOME_HTML
				"/assets/build/dist/main.js" -> MAIN_JS
				"/assets/build/dist/secure-test.js" -> SECURE_JS
				"/api/v1/manga" -> LIST_JSON
				"/api/v1/manga/8d9ye" -> DETAILS_JSON
				"/api/v1/manga/8d9ye/chapters" -> {
					assertEquals("test-token", url.queryParameter("_"))
					"""{"e":"chapters-fixture"}"""
				}
				"/api/v1/chapters/9074688" -> {
					assertEquals("test-token", url.queryParameter("_"))
					"""{"e":"pages-fixture"}"""
				}
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
		private const val HOME_HTML = """
			<html>
				<head>
					<meta name="cfg" content="cfg-test">
					<script type="module" src="/assets/build/dist/main.js"></script>
				</head>
			</html>
		"""

		private const val MAIN_JS = """import{i as g,n as v,t as b}from"./secure-test.js";"""

		private const val SECURE_JS = """export{g as i,v as n,b as t};"""

		private const val LIST_JSON = """
			{
				"status": "ok",
				"result": {
					"items": [
						{
							"hid": "8d9ye",
							"slug": "limitless-ascension-strength-amid-adversity",
							"title": "Limitless Ascension: Strength Amid Adversity",
							"synopsis": "Short synopsis",
							"poster": {
								"large": "https://img.example/poster-large.webp",
								"medium": "https://img.example/poster-medium.webp"
							},
							"status": "ongoing",
							"year": 2026,
							"ratedAvg": 8.4,
							"contentRating": "safe",
							"url": "https://www.comix.to/title/8d9ye-limitless-ascension-strength-amid-adversity"
						}
					]
				}
			}
		"""

		private const val DETAILS_JSON = """
			{
				"status": "ok",
				"result": {
					"hid": "8d9ye",
					"slug": "limitless-ascension-strength-amid-adversity",
					"title": "Limitless Ascension: Strength Amid Adversity",
					"synopsis": "Detailed synopsis",
					"altTitles": ["Boundless Climb"],
					"poster": {
						"large": "https://img.example/poster-large.webp"
					},
					"status": "ongoing",
					"year": 2026,
					"ratedAvg": 8.4,
					"contentRating": "safe",
					"genres": [
						{"id": 6, "name": "Action"},
						{"id": 12, "name": "Fantasy"}
					],
					"authors": [
						{"id": 1, "name": "Jane Writer"}
					],
					"artists": [
						{"id": 2, "name": "John Artist"}
					],
					"url": "https://www.comix.to/title/8d9ye-limitless-ascension-strength-amid-adversity"
				}
			}
		"""

		private const val CHAPTERS_JSON = """
			{
				"items": [
					{
						"id": 9074688,
						"mangaId": 61008,
						"number": 123,
						"volume": 0,
						"name": "",
						"group": {"id": 3, "name": "Drake Scans"},
						"url": "https://www.comix.to/title/8d9ye-limitless-ascension-strength-amid-adversity/9074688-chapter-123"
					},
					{
						"id": 8992182,
						"mangaId": 61008,
						"number": 122,
						"volume": 0,
						"name": "",
						"group": {"id": 3, "name": "Drake Scans"},
						"url": "https://www.comix.to/title/8d9ye-limitless-ascension-strength-amid-adversity/8992182-chapter-122"
					}
				],
				"meta": {
					"page": 1,
					"lastPage": 1
				}
			}
		"""

		private const val PAGES_JSON = """
			{
				"id": 9074688,
				"pages": {
					"baseUrl": "https://img.example/pages/",
					"items": [
						{"width": 800, "height": 1200, "url": "01.webp"},
						{"width": 800, "height": 1200, "url": "02.webp"}
					]
				}
			}
		"""
	}
}

package org.koitharu.kotatsu.parsers.site.fr

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
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.test_util.mangaOf

internal class PhenixscansParserTest {

	@Test
	fun `refreshes legacy manga URLs through the new API`() = runTest {
		val context = PhenixContext()
		val parser = PhenixscansParser(context)
		val details = parser.getDetails(
			mangaOf(MangaParserSource.PHENIXSCANS, "https://phenix-scans.com/manga/legacy-title"),
		)

		assertEquals("/manga/legacy-title", details.url)
		assertEquals("https://phenix-scans.co/manga/legacy-title", details.publicUrl)
		assertEquals("https://api.phenix-scans.co/covers/legacy.jpg", details.coverUrl)
		assertEquals(setOf("Romance"), details.tags.mapTo(mutableSetOf()) { it.title })

		val chapter = checkNotNull(details.chapters).single()
		assertEquals("/manga/legacy-title/chapter/1", chapter.url)
		val pages = parser.getPages(chapter)
		assertEquals(
			listOf(
				"https://api.phenix-scans.co/images/1.jpg",
				"https://cdn.phenix-scans.co/images/2.jpg",
			),
			pages.map { it.url },
		)
	}

	private class PhenixContext : MangaLoaderContext() {

		override val cookieJar: CookieJar = CookieJar.NO_COOKIES

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor(Interceptor { chain ->
				val request = chain.request()
				val body = when (request.url.encodedPath) {
					"/api/front/manga/legacy-title" -> DETAILS
					"/api/front/manga/legacy-title/chapter/1" -> PAGES
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
		const val DETAILS = """
			{
			  "manga": {
			    "slug": "legacy-title",
			    "title": "Legacy title",
			    "coverImage": "/covers/legacy.jpg",
			    "synopsis": "A migrated source",
			    "status": "Ongoing",
			    "averageRating": 8.0,
			    "genres": [{"id": "romance", "name": "Romance"}]
			  },
			  "chapters": [{
			    "_id": "chapter-1",
			    "number": "1",
			    "createdAt": "2026-09-18T00:00:00.000Z"
			  }]
			}
		"""

		const val PAGES = """
			{
			  "chapter": {
			    "images": ["/images/1.jpg", "https://cdn.phenix-scans.co/images/2.jpg"]
			  }
			}
		"""
	}
}

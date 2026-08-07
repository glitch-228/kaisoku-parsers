package org.koitharu.kotatsu.parsers.site.natsu

import kotlinx.coroutines.test.runTest
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.SourceConfigMock
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.site.natsu.id.Kiryuu
import java.io.IOException

class NatsuParserTest {

	@Test
	fun `details failures propagate and are not cached as incomplete manga`() {
		val context = FailingContext()
		val parser = Kiryuu(context)
		val manga = Manga(
			id = 1L,
			title = "Test",
			altTitles = emptySet(),
			url = "/manga/test/",
			publicUrl = "https://v6.kiryuu.to/manga/test/",
			rating = RATING_UNKNOWN,
			contentRating = ContentRating.SAFE,
			coverUrl = null,
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			source = MangaParserSource.KIRYUU,
		)

		repeat(2) {
			assertThrows(IOException::class.java) {
				runTest { parser.getDetails(manga) }
			}
		}
		assertEquals(2, context.requestCount)
	}

	private class FailingContext : MangaLoaderContext() {

		var requestCount = 0
			private set

		override val cookieJar: CookieJar = CookieJar.NO_COOKIES

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor {
				requestCount++
				throw IOException("Synthetic details failure")
			}
			.build()

		override fun getConfig(source: MangaSource): MangaSourceConfig = SourceConfigMock()

		override fun getDefaultUserAgent(): String = "test-agent"

		@Deprecated("Provide a base url")
		override suspend fun evaluateJs(script: String): String? = null

		override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null

		override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response {
			error("Not used")
		}

		override fun createBitmap(width: Int, height: Int): Bitmap {
			error("Not used")
		}
	}
}

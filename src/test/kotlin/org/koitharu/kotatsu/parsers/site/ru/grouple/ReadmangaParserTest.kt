package org.koitharu.kotatsu.parsers.site.ru.grouple

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
import org.koitharu.kotatsu.parsers.InMemoryCookieJar
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaLoaderContextMock
import org.koitharu.kotatsu.parsers.SourceConfigMock
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.SortOrder

internal class ReadmangaParserTest {

	@Test
	fun `query-only search does not request obsolete advanced filter form`() = runTest {
		val context = ReadmangaContext(domain = "a.zazaza.me")
		context.parser = ReadmangaParser(context)

		val result = context.parser.getList(0, SortOrder.RELEVANCE, MangaListFilter(query = "demo"))

		assertEquals(listOf("Demo Manga"), result.map { it.title })
		assertEquals(
			listOf("https://a.zazaza.me/search/advancedResults?q=demo&offset=0&years=1900%2C2099&sortType=RATING"),
			context.requestedUrls,
		)
	}

	@Test
	fun `ordinary 404 response is not reported as authorization required`() {
		val context = ReadmangaContext()
		context.parser = ReadmangaParser(context)
		val request = Request.Builder()
			.url("https://3.readmanga.ru/missing-chapter")
			.tag(MangaSource::class.java, context.parser.source)
			.build()

		context.httpClient.newCall(request).execute().use { response ->
			assertEquals(404, response.code)
		}
	}

	private class ReadmangaContext(
		private val domain: String? = null,
	) : MangaLoaderContext() {

		lateinit var parser: ReadmangaParser
		val requestedUrls = mutableListOf<String>()

		override val cookieJar: CookieJar = InMemoryCookieJar()

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor { chain -> parser.intercept(chain) }
			.addInterceptor(ReadmangaInterceptor(requestedUrls))
			.build()

		override fun getConfig(source: MangaSource): MangaSourceConfig {
			return domain?.let(::ReadmangaConfig) ?: SourceConfigMock()
		}

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

	private class ReadmangaConfig(
		private val domain: String,
	) : MangaSourceConfig {

		@Suppress("UNCHECKED_CAST")
		override fun <T> get(key: ConfigKey<T>): T {
			return if (key is ConfigKey.Domain) {
				domain as T
			} else {
				key.defaultValue
			}
		}
	}

	private class ReadmangaInterceptor(
		private val requestedUrls: MutableList<String>,
	) : Interceptor {

		override fun intercept(chain: Interceptor.Chain): Response {
			val request = chain.request()
			requestedUrls += request.url.toString()
			val body = when (request.url.encodedPath) {
				"/search/advancedResults" -> SEARCH_HTML
				"/search/advanced" -> error("Query-only search must not request the advanced filter form")
				else -> return response(request, code = 404, body = "Not found")
			}
			return response(request, code = 200, body = body)
		}

		private fun response(request: Request, code: Int, body: String): Response {
			return Response.Builder()
				.request(request)
				.protocol(Protocol.HTTP_1_1)
				.code(code)
				.message(if (code == 200) "OK" else "Not Found")
				.body(body.toResponseBody("text/html".toMediaType()))
				.build()
		}
	}

	private companion object {

		private const val SEARCH_HTML = """
			<div class="tiles row">
				<div class="tile">
					<div class="img">
						<a href="/demo-manga">
							<img class="lazy" data-original="https://rm.one-way.work/uploads/pics/demo_p.jpg" />
						</a>
					</div>
					<div class="desc">
						<h3><a href="/demo-manga">Demo Manga</a></h3>
						<div class="tile-info">
							<a class="person-link" href="/list/person/demo">Demo Author</a>
							<a class="element-link" href="/list/genre/action">боевик</a>
						</div>
					</div>
				</div>
			</div>
		"""
	}
}

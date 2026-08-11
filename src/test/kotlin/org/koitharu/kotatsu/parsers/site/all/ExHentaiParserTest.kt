package org.koitharu.kotatsu.parsers.site.all

import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.InMemoryCookieJar
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaLoaderContextMock
import org.koitharu.kotatsu.parsers.SourceConfigMock
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.insertCookies

internal class ExHentaiParserTest {

	@Test
	fun `exhentai unfiltered first page matches front page url`() = runTest {
		val context = ExHentaiContext(authorized = true)
		val parser = ExHentaiParser(context)

		assertEquals(emptyList<Any>(), parser.getList(0, SortOrder.NEWEST, MangaListFilter.EMPTY))

		// The mock returns NO_RESULTS_HTML (no gallery table), so the parser retries once with
		// inline_set=dm_e. Neither request may carry f_search or advsearch: an empty search turns
		// the front page into an empty advanced search returning a completely different set.
		assertEquals(
			listOf(
				"https://exhentai.org/",
				"https://exhentai.org/?inline_set=dm_e",
			),
			context.requestedUrls.map(HttpUrl::toString),
		)
		context.requestedUrls.forEach { url ->
			assertNull(url.queryParameter("f_search"))
			assertNull(url.queryParameter("advsearch"))
		}
	}

	@Test
	fun `exhentai search first page matches captured search url`() = runTest {
		val context = ExHentaiContext(authorized = true)
		val parser = ExHentaiParser(context)
		val filter = MangaListFilter(query = "Yaoi")

		assertEquals(emptyList<Any>(), parser.getList(0, SortOrder.NEWEST, filter))

		assertEquals(
			listOf(
				"https://exhentai.org/?f_search=Yaoi",
				"https://exhentai.org/?f_search=Yaoi&inline_set=dm_e",
			),
			context.requestedUrls.map(HttpUrl::toString),
		)
	}

	@Test
	fun `e-hentai first page preserves zero pagination cursor`() = runTest {
		val context = ExHentaiContext(authorized = false)
		val parser = ExHentaiParser(context)
		val filter = MangaListFilter(
			tags = setOf(MangaTag("Yaoi", "yaoi", parser.source)),
		)

		assertEquals(emptyList<Any>(), parser.getList(0, SortOrder.NEWEST, filter))

		assertEquals(2, context.requestedUrls.size)
		context.requestedUrls.forEach { url ->
			assertEquals("e-hentai.org", url.host)
			assertEquals("0", url.queryParameter("next"))
			assertEquals("tag:\"yaoi\"$", url.queryParameter("f_search"))
			assertEquals("1", url.queryParameter("advsearch"))
		}
	}

	@Test
	fun `e-hentai unfiltered page keeps advsearch but never sends empty f_search`() = runTest {
		val context = ExHentaiContext(authorized = false)
		val parser = ExHentaiParser(context)

		assertEquals(emptyList<Any>(), parser.getList(0, SortOrder.NEWEST, MangaListFilter.EMPTY))

		assertEquals(2, context.requestedUrls.size)
		context.requestedUrls.forEach { url ->
			assertEquals("e-hentai.org", url.host)
			assertEquals("0", url.queryParameter("next"))
			assertNull(url.queryParameter("f_search"))
			assertEquals("1", url.queryParameter("advsearch"))
		}
	}

	@Test
	fun `exhentai preserves titles from captured recent pages`() = runTest {
		val context = ExHentaiContext(authorized = true, responseHtml = CAPTURED_LIST_HTML)
		val parser = ExHentaiParser(context)

		val result = parser.getList(0, SortOrder.NEWEST, MangaListFilter.EMPTY)

		assertEquals(CAPTURED_TITLES, result.map { it.title })
	}

	@Test
	fun `exhentai preserves full titles on gallery details`() = runTest {
		val context = ExHentaiContext(
			authorized = true,
			responseHtml = CAPTURED_LIST_HTML,
			detailsHtml = CAPTURED_DETAILS_HTML,
		)
		val parser = ExHentaiParser(context)
		val manga = parser.getList(0, SortOrder.NEWEST, MangaListFilter.EMPTY).first()

		val result = parser.getDetails(manga)

		assertEquals(CAPTURED_TITLES.first(), result.title)
		assertEquals(setOf(CAPTURED_ALT_TITLE), result.altTitles)
	}

	@Test
	fun `e-hentai keeps existing simplified title behavior`() = runTest {
		val context = ExHentaiContext(authorized = false, responseHtml = CAPTURED_LIST_HTML)
		val parser = ExHentaiParser(context)

		val result = parser.getList(0, SortOrder.NEWEST, MangaListFilter.EMPTY)

		assertEquals(
			listOf(
				"✨Tadakuni & The Magicorps ✨ (OC by AlterKyon)",
				"Trap collection (various)",
				"(AI 翻译 粗翻) Shark cave",
			),
			result.map { it.title },
		)
	}

	private class ExHentaiContext(
		authorized: Boolean,
		responseHtml: String = NO_RESULTS_HTML,
		detailsHtml: String? = null,
	) : MangaLoaderContext() {

		val requestedUrls = mutableListOf<HttpUrl>()

		override val cookieJar = InMemoryCookieJar().apply {
			if (authorized) {
				insertCookies("e-hentai.org", "ipb_member_id=1", "ipb_pass_hash=test")
				insertCookies("exhentai.org", "ipb_member_id=1", "ipb_pass_hash=test")
			}
		}

		override val httpClient: OkHttpClient = OkHttpClient.Builder()
			.addInterceptor(ExHentaiInterceptor(requestedUrls, responseHtml, detailsHtml))
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

	private class ExHentaiInterceptor(
		private val requestedUrls: MutableList<HttpUrl>,
		private val responseHtml: String,
		private val detailsHtml: String?,
	) : Interceptor {

		override fun intercept(chain: Interceptor.Chain): Response {
			val request = chain.request()
			requestedUrls += request.url
			return Response.Builder()
				.request(request)
				.protocol(Protocol.HTTP_1_1)
				.code(200)
				.message("OK")
				.body(
					(if (request.url.encodedPath.startsWith("/g/")) detailsHtml ?: responseHtml else responseHtml)
						.toResponseBody("text/html".toMediaType()),
				)
				.build()
		}
	}

	private companion object {

		private const val NO_RESULTS_HTML = "<html><body>No hits found</body></html>"
		private val CAPTURED_TITLES = listOf(
			"[various] ✨Tadakuni & The Magicorps ✨ (OC by AlterKyon)",
			"[AlterKyon] Trap collection (various)",
			"(AI 翻译 粗翻)[Qundium/Hacony] Shark cave",
		)
		private const val CAPTURED_ALT_TITLE = "[合同] ただくに & マジックコープス (AlterKyon OC)"
		private val CAPTURED_LIST_HTML = """
			<html>
			<body>
			<table class="itg"><tbody>
			<tr>
				<td><img src="/1.webp"></td>
				<td><a href="/g/4084709/3ef59f76d8/">
					<div class="glink">[various] ✨Tadakuni &amp; The Magicorps ✨ (OC by AlterKyon)</div>
					<div><table><tbody></tbody></table></div>
				</a></td>
			</tr>
			<tr>
				<td><img src="/2.webp"></td>
				<td><a href="/g/4084708/5baf35419a/">
					<div class="glink">[AlterKyon] Trap collection (various)</div>
					<div><table><tbody></tbody></table></div>
				</a></td>
			</tr>
			<tr>
				<td><img src="/3.webp"></td>
				<td><a href="/g/4084136/c6cfc2ee75/">
					<div class="glink">(AI 翻译 粗翻)[Qundium/Hacony] Shark cave</div>
					<div><table><tbody></tbody></table></div>
				</a></td>
			</tr>
			</tbody></table>
			</body>
			</html>
		""".trimIndent()
		private val CAPTURED_DETAILS_HTML = """
			<html>
			<body>
			<div class="gm">
				<div id="gd2">
					<h1 id="gn">[various] ✨Tadakuni &amp; The Magicorps ✨ (OC by AlterKyon)</h1>
					<h1 id="gj">$CAPTURED_ALT_TITLE</h1>
				</div>
			</div>
			</body>
			</html>
		""".trimIndent()
	}
}

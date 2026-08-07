package org.koitharu.kotatsu.parsers.site.madara.vi

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.network.UserAgents
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.asTypedList
import org.koitharu.kotatsu.parsers.util.suspendlazy.getOrNull
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy
import java.util.UUID

// Do not use "hentaicb.sbs" domain, may cause duplicate tags!
@MangaSourceParser("HENTAICUBE", "CBHentai", "vi", ContentType.HENTAI)
internal class HentaiCube(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HENTAICUBE, "hentaicube.xyz") {

	override val datePattern = "dd/MM/yyyy"
	override val authorSearchSupported = true
	override val postDataReq = "action=manga_views&manga="

	override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
		.add("Origin", "https://$domain")
		.build()

	private val availableTags = suspendLazy(initializer = ::fetchTags)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = availableTags.get(),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val pages = page + 1

		val url = buildString {
			if (!filter.author.isNullOrEmpty()) {
				clear()
				append("https://")
				append(domain)
				append("/tacgia/")
				append(filter.author.lowercase().replace(" ", "-"))

				if (pages > 1) {
					append("/page/")
					append(pages.toString())
				}

				append("/?m_orderby=")
				when (order) {
					SortOrder.POPULARITY -> append("views")
					SortOrder.UPDATED -> append("latest")
					SortOrder.NEWEST -> append("new-manga")
					SortOrder.ALPHABETICAL -> {}
					SortOrder.RATING -> append("trending")
					SortOrder.RELEVANCE -> {}
					else -> append("latest") // default
				}
				return@buildString
			}

			append("https://")
			append(domain)

			if (pages > 1) {
				append("/page/")
				append(pages.toString())
			}

			append("/?s=")

			filter.query?.let {
				append(filter.query.urlEncoded())
			}

			append("&post_type=wp-manga")

			if (filter.tags.isNotEmpty()) {
				filter.tags.forEach {
					append("&genre[]=")
					append(it.key)
				}
			}

			filter.states.forEach {
				append("&status[]=")
				when (it) {
					MangaState.ONGOING -> append("on-going")
					MangaState.FINISHED -> append("end")
					MangaState.ABANDONED -> append("canceled")
					MangaState.PAUSED -> append("on-hold")
					MangaState.UPCOMING -> append("upcoming")
					else -> throw IllegalArgumentException("$it not supported")
				}
			}

			filter.contentRating.oneOrThrowIfMany()?.let {
				append("&adult=")
				append(
					when (it) {
						ContentRating.SAFE -> "0"
						ContentRating.ADULT -> "1"
						else -> ""
					},
				)
			}

			if (filter.year != 0) {
				append("&release=")
				append(filter.year.toString())
			}

			append("&m_orderby=")
			when (order) {
				SortOrder.POPULARITY -> append("views")
				SortOrder.UPDATED -> append("latest")
				SortOrder.NEWEST -> append("new-manga")
				SortOrder.ALPHABETICAL -> append("alphabet")
				SortOrder.RATING -> append("rating")
				SortOrder.RELEVANCE -> {}
				else -> {}
			}
		}
		return parseMangaList(webClient.httpGet(url).parseHtml())
	}

	override suspend fun createMangaTag(a: Element): MangaTag? {
		val allTags = availableTags.getOrNull().orEmpty()
		val title = a.text().replace(Regex("\\(\\d+\\)"), "").trim() // force trim to remove space
		// compare to avoid duplicate tags with the same title
		return allTags.find {
			it.title.trim().equals(title, ignoreCase = true) // try to search with trim
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val result = super.getDetails(manga)
		val doc = webClient.httpGet(result.publicUrl).parseHtml()
		return result.copy(
			title = doc.selectFirst("h1")?.ownText() ?: result.title,
		)
	}

	// Pages are now gated behind a reader token embedded in the chapter HTML: parse the
	// token, then request the image list with it (mirrors the dragonx/manga-repo fix; the old
	// v1 nonce challenge flow no longer works).
	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val html = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		val token = html.selectFirst(".manga-secure-reader")?.attr("data-masr2-token")
			?: throw ParseException("Web đã thay đổi thuật toán mã hóa ảnh, hết cứu!", chapter.url)

		val headers = Headers.Builder()
			.add("Referer", chapter.url.toAbsoluteUrl(domain))
			.add("User-Agent", UserAgents.CHROME_MOBILE)
			.add("Cookie", context.cookieJar.getCookies(domain).toString())
			.build()

		val url = urlBuilder().addPathSegments("wp-json/manga-reader/v2/pages")
			.addQueryParameter("token", token)
			.addQueryParameter("cid", randomHash())
			.build()

		val json = webClient.httpGet(url, headers).parseJson()
		return json.getJSONArray("items").asTypedList<String>().map {
			MangaPage(id = generateUid(it), url = it, preview = null, source = source)
		}
	}

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val headers = request.headers.newBuilder()
			.removeAll("Referer")
			.add("Referer", "https://$domain/")
			.add("User-Agent", UserAgents.CHROME_MOBILE)
			.build()

		val newRequest = request.newBuilder()
			.headers(headers)
			.build()

		return chain.proceed(newRequest)
	}

	private suspend fun fetchTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain/the-loai-genres").parseHtml()
		val elements = doc.select("ul.list-unstyled li a")
		return elements.mapToSet { element ->
			val href = element.attr("href")
			val key = href.substringAfter("/theloai/").removeSuffix("/")
			val title = element.text().replace(Regex("\\(\\d+\\)"), "").trim() // force trim
			MangaTag(
				key = key,
				title = title,
				source = source,
			)
		}.toSet()
	}

	private fun randomHash(): String = UUID.randomUUID().toString().replace("-", "")
}

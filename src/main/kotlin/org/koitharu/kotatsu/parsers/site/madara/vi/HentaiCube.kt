package org.koitharu.kotatsu.parsers.site.madara.vi

import okhttp3.Headers
import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.network.CloudFlareHelper
import org.koitharu.kotatsu.parsers.network.UserAgents
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.asTypedList
import org.koitharu.kotatsu.parsers.util.suspendlazy.getOrNull
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy

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

	// Pages are now gated behind a Cloudflare-backed nonce challenge: fetch a nonce, then request
	// the image list with it (mirrors the dragonx/manga-repo fix; plain reading-content scrape 403s).
	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val originUrl = chapter.url.substringBeforeLast("/ch").toAbsoluteUrl(domain)
		val referer = chapter.url.toAbsoluteUrl(domain)
		val cfCookies = "cf_chl_rc_ni=1; cf_clearance=" + CloudFlareHelper.getClearanceCookie(context.cookieJar, originUrl)
		val challengeHeaders = Headers.Builder()
			.add("Referer", referer)
			.add("User-Agent", UserAgents.CHROME_DESKTOP)
			.add("Cookie", cfCookies)
			.build()
		val nonce = webClient.httpGet(
			urlBuilder().addPathSegments("wp-json/manga-reader/v1/challenge").build(),
			challengeHeaders,
		).parseJson().getString("nonce")
		val imageHeaders = Headers.Builder()
			.add("Referer", referer)
			.add("User-Agent", UserAgents.CHROME_DESKTOP)
			.add("Cookie", cfCookies)
			.add("x-masr-nonce", nonce)
			.build()
		val json = webClient.httpGet(
			urlBuilder().addPathSegments("wp-json/manga-reader/v1/images").build(),
			imageHeaders,
		).parseJson()
		return json.getJSONArray("images").asTypedList<String>().map {
			MangaPage(id = generateUid(it), url = it, preview = null, source = source)
		}
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
}

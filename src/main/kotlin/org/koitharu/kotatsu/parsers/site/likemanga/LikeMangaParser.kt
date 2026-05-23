package org.koitharu.kotatsu.parsers.site.likemanga

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

internal abstract class LikeMangaParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	domain: String,
	pageSize: Int = 36,
) : PagedMangaParser(context, source, pageSize) {

	override val configKeyDomain = ConfigKey.Domain(domain)

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	// Always send the parser's domain root as Referer in addition to whatever the host app adds.
	// likemanga.ink's image CDN (like.mgread.io) enforces hotlink protection: requests without an
	// `https://likemanga.ink/...` Referer get a permanent CloudFlare block page, not a solvable
	// challenge. Sending Referer at the parser layer means it's locked in even if a downstream
	// interceptor (image proxy, etc.) strips the value the host added.
	override fun getRequestHeaders(): Headers = super.getRequestHeaders().newBuilder()
		.add("Referer", "https://$domain/")
		.build()

	// Image URLs returned from getPages() carry the originating chapter URL as a fragment
	// (e.g. https://like.mgread.io/.../1.png#https://likemanga.ink/onepunchman-8328/chapter-231/).
	// Strip the fragment here and use it as the Referer header on the actual request, so the CDN
	// sees a full, plausible referer instead of just the bare domain root that some CloudFlare WAF
	// rules treat as a generic hotlink attempt.
	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val fragment = request.url.fragment
		if (
			fragment != null &&
			(fragment.startsWith("https://") || fragment.startsWith("http://"))
		) {
			val cleanUrl = request.url.newBuilder().fragment(null).build()
			val newRequest = request.newBuilder()
				.url(cleanUrl)
				.header("Referer", fragment)
				.build()
			return chain.proceed(newRequest)
		}
		return chain.proceed(request)
	}

	override val availableSortOrders: Set<SortOrder> =
		EnumSet.of(
			SortOrder.UPDATED,
			SortOrder.POPULARITY,
			SortOrder.NEWEST,
			SortOrder.POPULARITY_TODAY,
			SortOrder.POPULARITY_WEEK,
			SortOrder.POPULARITY_MONTH,
		)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = fetchAvailableTags(),
		availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED, MangaState.PAUSED),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			append("/?act=search")

			filter.query?.let {
				append("&f")
				append("[keyword]".urlEncoded())
				append("=")
				append(filter.query.urlEncoded())
			}

			append("&f")
			append("[sortby]".urlEncoded())
			append("=")
			when (order) {
				SortOrder.UPDATED -> append("lastest-chap")
				SortOrder.NEWEST -> append("lastest-manga")
				SortOrder.POPULARITY -> append("top-manga")
				SortOrder.POPULARITY_TODAY -> append("top-day")
				SortOrder.POPULARITY_WEEK -> append("top-week")
				SortOrder.POPULARITY_MONTH -> append("top-month")
				else -> append("lastest-chap")
			}

			if (filter.tags.isNotEmpty()) {
				append("&f")
				append("[genres]".urlEncoded())
				append("=")
				filter.tags.oneOrThrowIfMany()?.let {
					append(it.key)
				}
			}

			filter.states.oneOrThrowIfMany()?.let {
				append("&f")
				append("[status]".urlEncoded())
				append("=")
				append(
					when (it) {
						MangaState.ONGOING -> "in-process"
						MangaState.FINISHED -> "complete"
						MangaState.PAUSED -> "pause"
						else -> "all"
					},
				)
			}

			if (page > 1) {
				append("&pageNum=")
				append(page)
			}

		}
		val doc = webClient.httpGet(url).parseHtml()
		return doc.select("div.card-body div.video").map { div ->
			val href = div.selectFirstOrThrow("a").attrAsRelativeUrl("href")
			Manga(
				id = generateUid(href),
				title = div.selectFirstOrThrow("p.title-manga").text(),
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = null,
				coverUrl = div.selectFirstOrThrow("img").src(),
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain/genres/").parseHtml()
		return doc.select("ul.nav-genres li:not(.text-center) a").mapToSet { a ->
			MangaTag(
				key = a.attr("href").removeSuffix('/').substringAfterLast('/'),
				title = a.text(),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val mangaId = manga.url.toAbsoluteUrl(domain).removeSuffix("/").substringAfterLast("-").toInt()
		val maxPageChapterSelect = doc.getElementById("nav_list_chapter_id_detail")?.select("a:not(.next)")
		var maxPageChapter = 1
		if (!maxPageChapterSelect.isNullOrEmpty()) {
			maxPageChapterSelect.map {
				val i = it.text().toInt()
				if (i > maxPageChapter) {
					maxPageChapter = i
				}
			}
		}
		val author = doc.selectLast("li.author p")?.textOrNull()
		return manga.copy(
			altTitles = setOfNotNull(doc.selectFirst(".list-info li.othername h2")?.textOrNull()),
			tags = doc.select("li.kind a").mapToSet { a ->
				MangaTag(
					key = a.attr("href").removeSuffix('/').substringAfterLast('/'),
					title = a.text().toTitleCase(),
					source = source,
				)
			},
			authors = setOfNotNull(author),
			description = doc.requireElementById("summary_shortened").html(),
			chapters = run {
				if (maxPageChapter == 1) {
					parseChapters(doc)
				} else {
					coroutineScope {
						val result = ArrayList(parseChapters(doc))
						result.ensureCapacity(result.size * maxPageChapter)
						(2..maxPageChapter).map { i ->
							async {
								loadChapters(mangaId, i)
							}
						}.awaitAll()
							.flattenTo(result)
						result
					}
				}
			}.reversed(),
		)
	}

	private suspend fun loadChapters(mangaId: Int, page: Int): List<MangaChapter> {
		val json =
			webClient.httpGet(
				"https://$domain/?act=ajax&code=load_list_chapter&manga_id=$mangaId&page_num=$page&chap_id=0&keyword=",
			)
				.parseJson().getString("list_chap")
		// Reuse parseChapters() so AJAX-loaded pages and the first page share the same DOM selectors;
		// drops the brittle text-mode substring slicing that broke on whitespace/escaping changes.
		val doc = Jsoup.parse(json)
		return parseChapters(doc)
	}

	private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
	private fun parseChapters(root: Element): List<MangaChapter> {
		return root.select("li.wp-manga-chapter")
			.map { li ->
				val url = li.selectFirstOrThrow("a").attrAsRelativeUrl("href")
				val dateText = if (li.selectFirstOrThrow(".chapter-release-date").text() == "New") {
					"today"
				} else {
					li.selectFirstOrThrow(".chapter-release-date").text()
				}
				val chapNum = url.substringAfter("chapter-").substringBefore("-")

				MangaChapter(
					id = generateUid(url),
					title = li.selectFirstOrThrow("a").text(),
					number = chapNum.toFloatOrNull() ?: 0f,
					volume = 0,
					url = url,
					scanlator = null,
					uploadDate = parseChapterDate(
						dateFormat,
						dateText,
					),
					branch = null,
					source = source,
				)
			}
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(fullUrl).parseHtml()
		val testJson = doc.selectFirst("div.reading input#next_img_token")
		val rawUrls: List<String> = if (testJson != null) {
			val jsonRaw = testJson.attr("value").split(".")[1]
			val jsonData = JSONObject(context.decodeBase64(jsonRaw).toString(Charsets.UTF_8))
			val jsonImg = context.decodeBase64(jsonData.getString("data")).toString(Charsets.UTF_8)
			val images = jsonImg.replace("\\", "").replace("[", "").replace("]", "").replace("\"", "").split(",")
			val baseUrl = doc.selectFirstOrThrow(".reading-detail  img").src()
			val cdn = baseUrl?.substringBefore("manga/", "")?.ifEmpty {
				baseUrl.toHttpUrl().resolve("/").toString()
			}
			images.map { img -> concatUrl(cdn.orEmpty(), img) }
		} else {
			doc.select(".reading-detail  img").map { img -> img.requireSrc() }
		}
		// Prime CloudFlare's __cf_bm cookie for the CDN host (like.mgread.io). Direct OkHttp
		// requests to the CDN get a 403 hotlink block until the cookie is set; a real browser
		// gets it for free because it fetches embedded <img> tags from the chapter page under
		// CloudFlare's normal "trusted navigation" allowance. Load the chapter URL in WebView
		// (sharing Android's CookieManager with our OkHttp via AndroidCookieJar) and wait for
		// the first chapter image to fire its load event — by then the Set-Cookie response is
		// stored. Best-effort: if the JS path fails or times out, we still return the page URLs
		// and let OkHttp try directly (it'll work for users CF doesn't gate-keep).
		runCatchingCancellable {
			context.evaluateJs(fullUrl, COOKIE_PRIME_SCRIPT, timeout = COOKIE_PRIME_TIMEOUT_MS)
		}
		// Tack the chapter URL onto the image URL fragment so intercept() can use it as Referer.
		// generateUid() runs on the bare URL so the page identity stays stable across visits.
		return rawUrls.map { url ->
			MangaPage(
				id = generateUid(url),
				url = "$url#$fullUrl",
				preview = null,
				source = source,
			)
		}
	}

	private fun parseChapterDate(dateFormat: DateFormat, date: String?): Long {
		val d = date?.lowercase() ?: return 0
		return when {
			d.startsWith("today") -> Calendar.getInstance().apply {
				set(Calendar.HOUR_OF_DAY, 0)
				set(Calendar.MINUTE, 0)
				set(Calendar.SECOND, 0)
				set(Calendar.MILLISECOND, 0)
			}.timeInMillis

			else -> dateFormat.parseSafe(date)
		}
	}

	private companion object {
		// Block until the first chapter image (or any element on the page) reports a load event.
		// CloudFlare sets __cf_bm via Set-Cookie on that image response, after which OkHttp can
		// read it through the shared CookieManager. Returns "ok" on first image load, "no-img"
		// if the page is empty, or "timeout"/"error" — any non-null/non-blank result unblocks
		// evaluateJs and lets getPages move on.
		private const val COOKIE_PRIME_SCRIPT = """
(async () => {
    try {
        const imgs = Array.from(document.querySelectorAll('.reading-detail img, img'));
        const cdnImgs = imgs.filter(i => (i.currentSrc || i.src || '').includes('mgread.io'));
        if (cdnImgs.length === 0) return 'no-img';
        const first = cdnImgs[0];
        if (first.complete && first.naturalWidth > 0) return 'ok';
        return await new Promise((resolve) => {
            first.addEventListener('load', () => resolve('ok'), { once: true });
            first.addEventListener('error', () => resolve('error'), { once: true });
            setTimeout(() => resolve('timeout'), 8000);
        });
    } catch (e) { return 'js-error:' + (e && e.message); }
})()
"""
		private const val COOKIE_PRIME_TIMEOUT_MS = 12_000L
	}
}

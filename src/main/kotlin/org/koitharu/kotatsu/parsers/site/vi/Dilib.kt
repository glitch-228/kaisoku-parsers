package org.koitharu.kotatsu.parsers.site.vi

import okhttp3.Headers
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.EnumSet

@MangaSourceParser("DILIB", "Dilib", "vi")
internal class Dilib(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.DILIB, 24) {

	override val configKeyDomain = ConfigKey.Domain("dilib.vn")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("Referer", "https://$domain/")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.POPULARITY,
		SortOrder.POPULARITY_ASC,
		SortOrder.NEWEST,
		SortOrder.NEWEST_ASC,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
			isAuthorSearchSupported = true,
			isMultipleTagsSupported = false,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = MAIN_CATEGORIES.mapTo(LinkedHashSet()) { (title, key) ->
			MangaTag(key = key, title = title, source = source)
		},
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://$domain/search.php?page=").append(page)
			append("&media=").append(BOOK_TYPE)
			append("&sort=").append(
				when (order) {
					SortOrder.POPULARITY -> "1"
					SortOrder.POPULARITY_ASC -> "2"
					SortOrder.NEWEST -> "3"
					SortOrder.NEWEST_ASC -> "4"
					else -> TRENDING_ORDER
				},
			)
			filter.tags.firstOrNull()?.let { append("&chinh=").append(it.key) }
			filter.author?.takeIf(String::isNotBlank)?.let { append("&author=").append(it.urlEncoded()) }
			filter.query?.takeIf(String::isNotBlank)?.let { append("&find=").append(it.urlEncoded()) }
		}
		return parseMangaList(webClient.httpGet(url).parseHtml())
	}

	private fun parseMangaList(doc: Document): List<Manga> =
		doc.select("div.products.row > div.type-product").mapNotNull { element ->
			val link = element.selectFirst(".block_product_thumbnail a, .block_product_content a")
				?: return@mapNotNull null
			val title = element.selectFirst(".block_product_content a")?.textOrNull()
				?: return@mapNotNull null
			val href = link.attrAsRelativeUrl("href")
			Manga(
				id = generateUid(href),
				title = title,
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = null,
				coverUrl = element.selectFirst(".block_product_thumbnail img")?.let { image ->
					image.attr("data-src").ifEmpty { image.attr("src") }.normalizeImageUrl()
				},
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}

	override suspend fun getDetails(manga: Manga): Manga {
		val url = manga.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(url).parseHtml()
		val updateTime = doc.selectFirst("p:contains(Cập nhật lúc)")?.ownText()?.trim()?.nullIfEmpty()
			?.let { "Cập nhật lúc: $it" }
		val description = listOfNotNull(
			updateTime,
			doc.selectFirst("div#content h2")?.textOrNull(),
			doc.selectFirst("div#content h2 + p")?.textOrNull(),
		).joinToString("\n\n").nullIfEmpty()
		val stateText = doc.selectFirst("p:contains(Tình trạng)")?.ownText()?.lowercase()
		val state = when {
			stateText == null -> null
			"đang cập nhật" in stateText -> MangaState.ONGOING
			"hoàn thành" in stateText -> MangaState.FINISHED
			else -> null
		}
		val tags = doc.select("fieldset#pdf a.button2").mapNotNullTo(LinkedHashSet()) { anchor ->
			val title = anchor.textOrNull() ?: return@mapNotNullTo null
			MangaTag(key = anchor.attr("href").substringAfterLast('/'), title = title, source = source)
		}
		return manga.copy(
			title = doc.selectFirst("div#primary h1")?.textOrNull() ?: manga.title,
			altTitles = emptySet(),
			authors = setOfNotNull(
				doc.selectFirst("div#primary h1 + p")?.textOrNull()
					?.substringAfter(':')?.trim()?.nullIfEmpty(),
			),
			description = description,
			tags = tags,
			state = state,
			coverUrl = doc.selectFirst("div#primary .size-shop_catalog img")?.attr("src")
				?.normalizeImageUrl() ?: manga.coverUrl,
			chapters = parseChapterList(doc),
		)
	}

	private suspend fun parseChapterList(doc: Document): List<MangaChapter> {
		val readUrl = (doc.selectFirst("a.button1[href*=-chap-]") ?: doc.selectFirst("a:contains(Đọc Truyện)"))
			?.attrAsAbsoluteUrlOrNull("href") ?: return emptyList()
		val baseChapterPath = readUrl.substringBefore("-chap-")
		return webClient.httpGet(readUrl).parseHtml().select("select option")
			.mapNotNull { option ->
				val value = option.attr("value")
				if (!value.contains("-chap-", ignoreCase = true)) return@mapNotNull null
				val title = option.textOrNull() ?: return@mapNotNull null
				val url = "$baseChapterPath$value.html"
				MangaChapter(
					id = generateUid(url),
					title = title,
					number = CHAPTER_NUMBER.find(title)?.value?.toFloatOrNull() ?: 0f,
					volume = 0,
					url = url.toRelativeUrl(domain),
					scanlator = null,
					uploadDate = 0L,
					branch = null,
					source = source,
				)
			}.distinctBy { it.url }.sortedBy { it.number }
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val url = chapter.url.toAbsoluteUrl(domain)
		val pages = webClient.httpGet(url).parseHtml().select("div#primary > img.border")
			.mapNotNull { image ->
				image.attr("data-src").ifEmpty { image.attr("src") }
					.replace("\r", "").nullIfEmpty()?.toAbsoluteUrl(domain)
			}.distinct()
		if (pages.isEmpty()) throw ParseException("Could not find image data", url)
		return pages.map { pageUrl ->
			MangaPage(id = generateUid(pageUrl), url = pageUrl, preview = null, source = source)
		}
	}

	private fun String.normalizeImageUrl(): String = when {
		startsWith("//") -> "https:$this"
		startsWith("/") -> toAbsoluteUrl(domain)
		else -> this
	}

	private companion object {

		const val BOOK_TYPE = "5"
		const val TRENDING_ORDER = "5"
		val CHAPTER_NUMBER = Regex("""\d+(?:\.\d+)?""")
		val MAIN_CATEGORIES = arrayOf(
			"Manga" to "manga", "Manhua" to "manhua", "Manhwa" to "manhwa", "Action" to "action",
			"Adventure" to "adventure", "Comedy" to "comedy", "Fantasy" to "fantasy", "Shounen" to "shounen",
			"Shoujo" to "shoujo", "Supernatural" to "supernatural", "Sci-Fi" to "sci-fi", "Martial Arts" to "martial-arts",
			"Seinen" to "seinen", "Drama" to "drama", "Mystery" to "mystery", "Cooking" to "cooking",
			"Harem" to "harem", "Romance" to "romance", "School Life" to "school-life", "Historical" to "historical",
			"Psychological" to "psychological", "Tragedy" to "tragedy", "Truyện Màu" to "truyen-mau", "Horror" to "horror",
			"Slice Of Life" to "slice-of-life", "Adult (18+)" to "adult-18", "Sports" to "sports", "Ecchi" to "ecchi",
			"Webtoon" to "webtoon", "Mature" to "mature", "Tu Tiên" to "tu-tien", "Vampire" to "vampire",
			"Josei" to "josei", "Xuyên Không" to "xuyen-khong", "Magic" to "magic", "Monsters" to "monsters",
			"Hệ Thống" to "he-thong", "Thriller" to "thriller",
		)
	}
}

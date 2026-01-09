package org.koitharu.kotatsu.parsers.site.ar

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("AZORAMOON", "Azoramoon", "ar")
internal class Azoramoon(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.AZORAMOON, 24) {

	override val availableSortOrders: Set<SortOrder> =
		EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY, SortOrder.ALPHABETICAL)

	override val configKeyDomain = ConfigKey.Domain("azoramoon.com")

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = fetchAvailableTags(),
	)

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			append("/series/")

			val params = mutableListOf<String>()

			// Add search query
			if (!filter.query.isNullOrEmpty()) {
				params.add("searchTerm=${filter.query!!.urlEncoded()}")
			}

			// Add genre filters
			if (filter.tags.isNotEmpty()) {
				val genreIds = filter.tags.joinToString(",") { it.key }
				params.add("genres=%2B$genreIds")
			}

			// Add sort filter
			params.add("sortBy=" + when (order) {
				SortOrder.UPDATED -> "newest"
				SortOrder.POPULARITY -> "popular"
				SortOrder.ALPHABETICAL -> "alphabetical"
				else -> "newest"
			})

			// Add page parameter (if pagination is supported)
			if (page > 1) {
				params.add("page=$page")
			}

			// Append parameters
			if (params.isNotEmpty()) {
				append("?")
				append(params.joinToString("&"))
			}
		}

		val doc = webClient.httpGet(url).parseHtml()

		// Parse manga cards from the grid layout
		return doc.select("section > div > a[href*='/series/']").mapNotNull { card ->
			val href = card.attrAsRelativeUrlOrNull("href") ?: return@mapNotNull null
			val img = card.selectFirst("img") ?: return@mapNotNull null
			val coverUrl = img.src()
			val title = img.attr("alt").ifEmpty {
				card.selectFirst("h3")?.text() ?: return@mapNotNull null
			}

			Manga(
				id = generateUid(href),
				title = title,
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = null,
				coverUrl = coverUrl,
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain/series/").parseHtml()

		// Extract genres from the page
		return doc.select("button[data-genre-id], a[href*='genres=']").mapNotNullToSet { element ->
			val genreId = element.attr("data-genre-id").ifEmpty {
				element.attr("href").substringAfter("genres=").substringBefore("&")
			}
			val genreName = element.text().trim()

			if (genreId.isNotEmpty() && genreName.isNotEmpty()) {
				MangaTag(
					key = genreId,
					title = genreName,
					source = source,
				)
			} else {
				null
			}
		}
	}

	override suspend fun getDetails(manga: Manga): Manga = coroutineScope {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val chaptersDeferred = async { loadChapters(manga.url) }

		val coverUrl = doc.selectFirst("img[alt*='${manga.title}'], section img")?.src() ?: manga.coverUrl

		// Extract rating
		val ratingText = doc.selectFirst("div:contains(التقييم) + div, span:contains(Rating)")?.text()
		val rating = ratingText?.substringBefore("/")?.trim()?.toFloatOrNull()?.div(5f) ?: RATING_UNKNOWN

		// Extract status
		val statusText = doc.selectFirst("div:contains(الحالة) + div, span:contains(Status)")?.text()
		val state = when {
			statusText?.contains("مستمر", ignoreCase = true) == true ||
			statusText?.contains("ongoing", ignoreCase = true) == true -> MangaState.ONGOING
			statusText?.contains("مكتمل", ignoreCase = true) == true ||
			statusText?.contains("completed", ignoreCase = true) == true -> MangaState.FINISHED
			else -> null
		}

		// Extract description
		val description = doc.selectFirst("div.text-sm.text-gray-600, div.description, p.summary")?.html()

		// Extract tags/genres
		val tags = doc.select("a[href*='/series/?genres='], span.genre").mapNotNullToSet { element ->
			val genreName = element.text().trim()
			val genreId = element.attr("href").substringAfter("genres=").substringBefore("&")
				.ifEmpty { genreName }

			if (genreName.isNotEmpty()) {
				MangaTag(
					key = genreId,
					title = genreName,
					source = source,
				)
			} else {
				null
			}
		}

		manga.copy(
			coverUrl = coverUrl,
			rating = rating,
			state = state,
			tags = tags,
			description = description,
			chapters = chaptersDeferred.await(),
		)
	}

	private suspend fun loadChapters(mangaUrl: String): List<MangaChapter> {
		val doc = webClient.httpGet(mangaUrl.toAbsoluteUrl(domain)).parseHtml()
		val dateFormat = SimpleDateFormat("yyyy-MM-dd", sourceLocale)

		return doc.select("div.mt-4.space-y-2 a[href*='/chapter/'], div.chapter-list a").mapChapters { i, a ->
			val url = a.attrAsRelativeUrl("href")
			val titleElement = a.selectFirst("span.font-semibold, span.chapter-title")
			val title = titleElement?.text() ?: a.text().substringBefore("•").trim()

			// Extract chapter number from title or URL
			val chapterNumber = title.substringAfter("الفصل", "")
				.substringAfter("Chapter", "")
				.trim()
				.split(" ")[0]
				.toFloatOrNull() ?: (i + 1f)

			// Extract date
			val dateElement = a.selectFirst("time, span.text-gray-500")
			val dateText = dateElement?.attr("datetime") ?: dateElement?.text()

			MangaChapter(
				id = generateUid(url),
				title = title,
				number = chapterNumber,
				volume = 0,
				url = url,
				scanlator = null,
				uploadDate = dateFormat.parseSafe(dateText),
				branch = null,
				source = source,
			)
		}
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(fullUrl).parseHtml()

		return doc.select("div.comic-images-wrapper img, div.chapter-images img, img[data-index]")
			.mapNotNull { img ->
				val imageUrl = img.attr("data-src").ifEmpty { img.src() }
				if (imageUrl?.isNotBlank() == true && !imageUrl.startsWith("data:image")) {
					val finalUrl = imageUrl.toRelativeUrl(domain)
					MangaPage(
						id = generateUid(finalUrl),
						url = finalUrl,
						preview = null,
						source = source,
					)
				} else {
					null
				}
			}
			.distinct()
	}

}

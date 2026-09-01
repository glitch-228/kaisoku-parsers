package org.koitharu.kotatsu.parsers.site.en

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.model.WordSet
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.*
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.EnumSet
import java.util.LinkedHashMap
import java.util.Locale
import java.util.TreeMap

@MangaSourceParser("ASURASCANS", "Asura Scans", "en")
internal class AsuraScansParser(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.ASURASCANS, pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("asurascans.com")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.UPDATED_ASC,
		SortOrder.POPULARITY,
		SortOrder.POPULARITY_ASC,
		SortOrder.RATING,
		SortOrder.RATING_ASC,
		SortOrder.ALPHABETICAL_DESC,
		SortOrder.ALPHABETICAL,
		SortOrder.NEWEST,
		SortOrder.NEWEST_ASC,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isMultipleTagsSupported = true,
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
			isAuthorSearchSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = availableTags.get(),
		availableStates = EnumSet.of(
			MangaState.ONGOING,
			MangaState.FINISHED,
			MangaState.ABANDONED,
			MangaState.PAUSED,
		),
		availableContentTypes = EnumSet.of(
			ContentType.MANGA,
			ContentType.MANHWA,
			ContentType.MANHUA,
		),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = "https://$domain/browse".toHttpUrl().newBuilder().apply {
			addQueryParameter("page", page.toString())

			if (!filter.query.isNullOrBlank()) {
				addQueryParameter("search", filter.query)
			}

			if (filter.tags.isNotEmpty()) {
				addQueryParameter("genres", filter.tags.joinToString(",") { it.key })
			}

			filter.states.oneOrThrowIfMany()?.let {
				addQueryParameter(
					"status",
					when (it) {
						MangaState.ONGOING -> "ongoing"
						MangaState.FINISHED -> "completed"
						MangaState.ABANDONED -> "dropped"
						MangaState.PAUSED -> "hiatus"
						else -> throw IllegalArgumentException("$it not supported")
					},
				)
			}

			filter.types.oneOrThrowIfMany()?.let {
				addQueryParameter(
					"types",
					when (it) {
						ContentType.MANGA -> "manga"
						ContentType.MANHWA -> "manhwa"
						ContentType.MANHUA -> "manhua"
						else -> throw IllegalArgumentException("$it not supported")
					},
				)
			}

			if (!filter.author.isNullOrBlank()) {
				addQueryParameter("author", filter.author)
			}

			val (sort, ascending) = order.toAsuraSortOrder()
			if (!sort.isNullOrEmpty()) {
				addQueryParameter("sort", sort)
			}
			if (ascending) {
				addQueryParameter("order", "asc")
			}
		}.build()
		val doc = webClient.httpGet(url).parseHtml()
		return doc.select("#series-grid .series-card").mapNotNull { card ->
			val link = card.selectFirst("a[href*=/comics/]") ?: return@mapNotNull null
			val href = link.attrAsRelativeUrl("href").stripSlugToken()
			Manga(
				id = generateUid(href),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				coverUrl = card.selectFirst("img")?.src(),
				title = card.selectFirst("h3")?.text()?.trim().orEmpty(),
				altTitles = emptySet(),
				rating = card.selectFirst("div.absolute.top-2.right-2 span")?.text()?.toFloatOrNull() ?: RATING_UNKNOWN,
				tags = emptySet(),
				authors = emptySet(),
				state = when (card.select("div.p-3 span").lastOrNull()?.text()?.trim()?.lowercase(Locale.ENGLISH)) {
					"ongoing" -> MangaState.ONGOING
					"completed" -> MangaState.FINISHED
					"hiatus" -> MangaState.PAUSED
					"dropped" -> MangaState.ABANDONED
					"coming soon" -> MangaState.UPCOMING
					else -> null
				},
				source = source,
				contentRating = if (isNsfwSource) ContentRating.ADULT else null,
			)
		}
	}

	private val availableTags = suspendLazy(initializer = ::fetchAvailableTags)

	private val tagMap = suspendLazy {
		val tags = availableTags.get()
		tags.associateByTo(LinkedHashMap(tags.size)) {
			it.title.lowercase(Locale.ENGLISH)
		}
	}

	private val regexDate = """(\d+)(st|nd|rd|th)""".toRegex()
	private val chapterDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val availableTagMap = tagMap.get()
		val selectTag = doc.select("div[class^=space] > div.flex > button.text-white")
		val tags = selectTag.mapNotNullToSet { element ->
			val title = element.text().trim().nullIfEmpty() ?: return@mapNotNullToSet null
			availableTagMap[title.lowercase(Locale.ENGLISH)] ?: MangaTag(
				key = title.toAsuraGenreKey(),
				title = title,
				source = source,
			)
		}
		val author = doc.selectFirst("div.grid > div:has(h3:eq(0):containsOwn(Author)) > h3:eq(1)")
			?.text()
			?.trim()
			?.nullIfEmpty()
		val cutoffTime = System.currentTimeMillis() - CHAPTER_HIDE_WINDOW_MS
		return manga.copy(
			title = doc.selectFirst("article h1")
				?.text()
				?.trim()
				?.takeIf { it.isNotEmpty() }
				?: manga.title,
			altTitles = doc.selectFirst("#alt-titles")
				?.text()
				.orEmpty()
				.split('•', '\n')
				.mapNotNullToSet { it.trim().nullIfEmpty() },
			description = doc.selectFirst("#description-text")
				?.html()
				?.trim()
				?.takeIf { it.isNotEmpty() }
				?: doc.selectFirst("span.font-medium.text-sm")?.text().orEmpty(),
			tags = tags,
			authors = setOfNotNull(author),
			chapters = doc.select("a.group[href*=/chapter/]").mapChapters(reversed = true) { i, a ->
				val urlRelative = a.attrAsRelativeUrl("href").stripSlugToken()
				val titleElement = a.selectFirst("span.font-medium") ?: a.selectFirst("span")
				val chapterLabel = titleElement?.text()?.trim()?.takeIf { it.isNotEmpty() }
				val chapterTitle = a.selectFirst("span.text-sm.text-white\\/50")
					?.text()
					?.trim()
					?.takeIf { it.isNotEmpty() }
				val fullTitle = when {
					chapterLabel != null && chapterTitle != null -> "$chapterLabel - $chapterTitle"
					chapterLabel != null -> chapterLabel
					else -> chapterTitle
				}
				val chapterNumber = chapterNumberRegex.find(chapterLabel.orEmpty())
					?.groupValues
					?.getOrNull(1)
					?.toFloatOrNull()
					?: i + 1f
				val dateText = a.selectFirst("span.text-sm.text-white\\/40")
					?.text()
					?.trim()
				MangaChapter(
					id = generateUid(urlRelative),
					title = fullTitle,
					number = chapterNumber,
					volume = 0,
					url = urlRelative,
					scanlator = null,
					uploadDate = parseChapterDate(chapterDateFormat, dateText),
					branch = null,
					source = source,
				)
			}.filter { chapter ->
				chapter.uploadDate == 0L || chapter.uploadDate <= cutoffTime
			},
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		doc.selectFirst("astro-island[component-url*='ChapterReader']")?.attr("props")?.let { props ->
			pageUrlRegex.findAll(props.replace("&quot;", "\""))
				.map { it.groupValues[1] }
				.distinct()
				.toList()
				.takeIf { it.isNotEmpty() }
				?.let { urls ->
					return urls.map { url ->
						MangaPage(
							id = generateUid(url),
							url = url,
							preview = null,
							source = source,
						)
					}
				}
		}

		val scripts = doc.selectOrThrow("script")
		val sb = StringBuilder()
		for (script in scripts) {
			val raw = script.data().substringBetween("self.__next_f.push(", ")", "").trim()
			if (raw.isEmpty()) continue
			val ja = raw.toJSONArrayOrNull() ?: continue
			for (i in 0 until ja.length()) {
				(ja.opt(i) as? String)?.let { sb.append(it) }
			}
		}
		val lines = sb.toString().split('\n')
		val pages = TreeMap<Int, String>()
		for (line in lines) {
			val obj = line.substringAfter(':').toJSONObjectOrNull() ?: continue
			if (obj.has("order") && obj.has("url")) {
				pages[obj.getInt("order")] = obj.getString("url")
			}
		}
		return pages.values.map { url ->
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		val json = webClient.httpGet("https://api.$domain/api/genres").parseJson()
		val genres = json.optJSONArray("data") ?: return emptySet()
		return genres.mapJSONToSet { jo ->
			MangaTag(
				key = jo.getString("slug"),
				title = jo.getString("name"),
				source = source,
			)
		}
	}

	private fun SortOrder.toAsuraSortOrder(): Pair<String?, Boolean> = when (this) {
		SortOrder.UPDATED -> null to false
		SortOrder.UPDATED_ASC -> null to true
		SortOrder.POPULARITY -> "popular" to false
		SortOrder.POPULARITY_ASC -> "popular" to true
		SortOrder.RATING -> "rating" to false
		SortOrder.RATING_ASC -> "rating" to true
		SortOrder.ALPHABETICAL_DESC -> "name" to false
		SortOrder.ALPHABETICAL -> "name" to true
		SortOrder.NEWEST -> "newest" to false
		SortOrder.NEWEST_ASC -> "newest" to true
		else -> null to false
	}

	private fun String.toAsuraGenreKey(): String {
		return trim()
			.lowercase(Locale.ENGLISH)
			.replace(asuraGenreKeyRegex, "-")
			.trim('-')
	}

	private fun parseChapterDate(dateFormat: DateFormat, date: String?): Long {
		val value = date?.trim().orEmpty()
		if (value.isEmpty()) return 0L
		val lower = value.lowercase(Locale.ENGLISH)
		return when {
			lower == "last week" -> Calendar.getInstance().apply {
				add(Calendar.WEEK_OF_YEAR, -1)
			}.timeInMillis

			lower == "yesterday" -> Calendar.getInstance().apply {
				add(Calendar.DAY_OF_MONTH, -1)
			}.timeInMillis

			lower.endsWith(" ago") -> parseRelativeDate(lower)
			else -> synchronized(dateFormat) {
				dateFormat.parseSafe(value.replace(regexDate, "$1"))
			}
		}
	}

	private fun parseRelativeDate(date: String): Long {
		val number = Regex("""(\d+)""").find(date)?.value?.toIntOrNull() ?: return 0L
		val cal = Calendar.getInstance()
		return when {
			WordSet("second", "seconds").anyWordIn(date) -> cal.apply { add(Calendar.SECOND, -number) }.timeInMillis
			WordSet("minute", "minutes", "min", "mins").anyWordIn(date) -> cal.apply { add(Calendar.MINUTE, -number) }.timeInMillis
			WordSet("hour", "hours").anyWordIn(date) -> cal.apply { add(Calendar.HOUR, -number) }.timeInMillis
			WordSet("day", "days").anyWordIn(date) -> cal.apply { add(Calendar.DAY_OF_MONTH, -number) }.timeInMillis
			WordSet("week", "weeks").anyWordIn(date) -> cal.apply { add(Calendar.WEEK_OF_YEAR, -number) }.timeInMillis
			WordSet("month", "months").anyWordIn(date) -> cal.apply { add(Calendar.MONTH, -number) }.timeInMillis
			WordSet("year", "years").anyWordIn(date) -> cal.apply { add(Calendar.YEAR, -number) }.timeInMillis
			else -> 0L
		}
	}

	/**
	 * Asura appends the same site-wide token to every series slug
	 * (`/comics/<slug>-00dcbf97`, identical across all series) and rotates it
	 * whenever the site is redeployed — which is exactly when new chapters go
	 * up. Any id derived from a url containing it therefore changes for every
	 * chapter of every series at once, so chapters already downloaded come back
	 * as new, undownloaded ones.
	 *
	 * The token is not needed to reach anything: the server answers a slug with
	 * a missing or stale token with a 302 to the current url, so it is dropped
	 * from the stored url and, with it, from the generated ids.
	 */
	private fun String.stripSlugToken(): String = replace(slugTokenRegex, "")

	private companion object {
		private const val CHAPTER_HIDE_WINDOW_MS = 6L * 60L * 60L * 1000L
		private val slugTokenRegex = Regex("""-[0-9a-f]{8}(?=/|$)""")
		private val chapterNumberRegex = Regex("""Chapter\s+(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
		private val pageUrlRegex = Regex(""""url":\s*\[0,\s*"([^"]+)"""")
		private val asuraGenreKeyRegex = Regex("[^a-z0-9]+")
	}
}

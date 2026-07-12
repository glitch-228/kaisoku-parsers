package org.koitharu.kotatsu.parsers.site.ru

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.ContentType
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
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.json.mapJSONToSet
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.parseSafe
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.EnumSet
import java.util.Locale

@MangaSourceParser("ASTRAMANGA", "AstraManga", "ru", ContentType.MANGA)
internal class AstraManga(
	context: MangaLoaderContext,
) : PagedMangaParser(context, MangaParserSource.ASTRAMANGA, PAGE_SIZE) {

	override val configKeyDomain = ConfigKey.Domain("astramanga.org")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.POPULARITY,
		SortOrder.UPDATED,
		SortOrder.NEWEST,
		SortOrder.RATING,
		SortOrder.ALPHABETICAL,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isMultipleTagsSupported = true,
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
		)

	override fun getRequestHeaders(): Headers = super.getRequestHeaders().newBuilder()
		.add("Accept", "application/json")
		.add("Origin", SITE_URL)
		.add("Referer", "$SITE_URL/")
		.build()

	override fun intercept(chain: Interceptor.Chain): Response {
		return chain.proceed(
			chain.request().newBuilder()
				.header("Referer", "$SITE_URL/")
				.header("Origin", SITE_URL)
				.build(),
		)
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = "$API_URL/search".toHttpUrl().newBuilder()
			.addQueryParameter("page", page.toString())
			.addQueryParameter("size", PAGE_SIZE.toString())
			.apply {
				filter.query?.trim()?.takeIf { it.isNotEmpty() }?.let {
					addQueryParameter("query", it)
				}
				addQueryParameter("sort", order.toApiSort())
				filter.states.mapNotNull { it.toApiStatus() }.forEach {
					addQueryParameter("status", it)
				}
				filter.tags.forEach {
					addQueryParameter("genres", it.key)
				}
			}
			.build()
		return webClient.httpGet(url, getRequestHeaders())
			.parseJson()
			.optJSONObject("data")
			?.optJSONArray("titles")
			?.mapJSON { it.toManga() }
			.orEmpty()
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val slug = manga.url.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: return manga
		val title = webClient.httpGet("$API_URL/titles/$slug", getRequestHeaders())
			.parseJson()
			.optJSONObject("data")
			?: return manga
		val id = title.optInt("id", 0).takeIf { it > 0 } ?: return manga
		val branch = webClient.httpGet("$API_URL/titles/$id/branches", getRequestHeaders())
			.parseJson()
			.optJSONObject("data")
			?.optJSONArray("branches")
			?.firstMainBranch()
			?: return manga.copy(
				title = title.getStringOrNull("name") ?: manga.title,
				description = title.getStringOrNull("description"),
				state = title.getStringOrNull("status").toMangaState(),
				tags = title.optJSONArray("genres").toTags(),
			)
		val chapters = webClient.httpGet("$API_URL/branches/${branch.optInt("id")}/chapters", getRequestHeaders())
			.parseJson()
			.optJSONObject("data")
			?.optJSONArray("items")
			?.mapJSON { it.toChapter() }
			.orEmpty()
			.sortedBy { it.number }

		return manga.copy(
			title = title.getStringOrNull("name") ?: manga.title,
			description = title.getStringOrNull("description"),
			state = title.getStringOrNull("status").toMangaState(),
			chapters = chapters,
			tags = title.optJSONArray("genres").toTags(),
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		return webClient.httpGet(chapter.url, getRequestHeaders())
			.parseJson()
			.optJSONObject("data")
			?.optJSONArray("pages")
			?.mapJSON { page ->
				val imageUrl = page.getStringOrNull("image_url").orEmpty()
				MangaPage(
					id = generateUid(imageUrl),
					url = imageUrl,
					preview = null,
					source = source,
				)
			}
			.orEmpty()
			.filter { it.url.isNotEmpty() }
	}

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions(
		availableTags = GENRES.mapTo(LinkedHashSet()) {
			MangaTag(it.first, it.second, source)
		},
		availableStates = EnumSet.of(
			MangaState.ONGOING,
			MangaState.FINISHED,
		),
	)

	private fun JSONObject.toManga(): Manga {
		val slug = getStringOrNull("slug").orEmpty()
		val url = "/manga/$slug"
		return Manga(
			id = generateUid("$slug-${optInt("id", 0)}"),
			title = getStringOrNull("name") ?: slug,
			altTitles = emptySet(),
			url = url,
			publicUrl = "$SITE_URL$url",
			rating = RATING_UNKNOWN,
			contentRating = null,
			coverUrl = null,
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			source = source,
		)
	}

	private fun JSONObject.toChapter(): MangaChapter {
		val chapterId = optLong("id", 0L)
		val number = optDouble("number", 0.0).toFloat()
		return MangaChapter(
			id = generateUid(chapterId.toString()),
			title = if (number > 0f) "Глава $number" else null,
			number = number,
			volume = 0,
			url = "$API_URL/chapters/$chapterId/pages",
			scanlator = null,
			uploadDate = parseDate(getStringOrNull("published_at")),
			branch = null,
			source = source,
		)
	}

	private fun JSONArray?.toTags(): Set<MangaTag> {
		return this?.mapJSONToSet {
			MangaTag(
				title = it.getStringOrNull("name").orEmpty(),
				key = it.getStringOrNull("slug").orEmpty(),
				source = source,
			)
		}.orEmpty().filterTo(LinkedHashSet()) { it.title.isNotEmpty() && it.key.isNotEmpty() }
	}

	private fun JSONArray.firstMainBranch(): JSONObject? {
		for (i in 0 until length()) {
			val branch = optJSONObject(i) ?: continue
			if (branch.optBoolean("is_main", false)) {
				return branch
			}
		}
		return if (length() > 0) optJSONObject(0) else null
	}

	private fun String?.toMangaState(): MangaState? = when (this?.lowercase(Locale.ROOT)) {
		"ongoing" -> MangaState.ONGOING
		"completed" -> MangaState.FINISHED
		else -> null
	}

	private fun MangaState.toApiStatus(): String? = when (this) {
		MangaState.ONGOING -> "ongoing"
		MangaState.FINISHED -> "completed"
		else -> null
	}

	private fun SortOrder.toApiSort(): String = when (this) {
		SortOrder.POPULARITY -> "-popularity"
		SortOrder.UPDATED -> "-updated_at"
		SortOrder.NEWEST -> "-created_at"
		SortOrder.RATING -> "-rating"
		SortOrder.ALPHABETICAL -> "-name"
		else -> "-popularity"
	}

	private fun parseDate(date: String?): Long {
		val value = date?.trim().orEmpty()
		if (value.isEmpty()) {
			return 0L
		}
		return runCatching {
			OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli()
		}.getOrDefault(DATE_FORMAT.parseSafe(value))
	}

	private companion object {
		private const val PAGE_SIZE = 30
		private const val API_URL = "https://api.astramanga.org/api/v1"
		private const val SITE_URL = "https://astramanga.org"

		private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
		private val GENRES = listOf(
			"Action" to "action",
			"Adventure" to "adventure",
			"Drama" to "drama",
			"Fantasy" to "fantasy",
			"Romance" to "romance",
			"Horror" to "horror",
			"Comedy" to "comedy",
			"Isekai" to "isekai",
		)
	}
}

package org.koitharu.kotatsu.parsers.site.fr

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrl
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
import org.koitharu.kotatsu.parsers.util.oneOrThrowIfMany
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.parseSafe
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNull
import org.koitharu.kotatsu.parsers.util.json.mapJSONToSet
import java.text.SimpleDateFormat
import java.util.EnumSet
import java.util.Locale

private const val API_HOST = "api.phenix-scans.co"
private const val API_BASE = "https://$API_HOST/api"
private const val WEB_BASE = "https://phenix-scans.co"
private const val PAGE_SIZE = 18

@MangaSourceParser("PHENIXSCANS", "PhenixScans", "fr")
internal class PhenixscansParser(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.PHENIXSCANS, PAGE_SIZE) {

	override val configKeyDomain = ConfigKey.Domain("phenix-scans.co")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.ALPHABETICAL,
		SortOrder.RATING,
		SortOrder.UPDATED,
		SortOrder.NEWEST,
		SortOrder.POPULARITY,
	)

	override val filterCapabilities = MangaListFilterCapabilities(
		isSearchSupported = true,
		isMultipleTagsSupported = true,
		isSearchWithFiltersSupported = true,
	)

	private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ROOT)
	private val tagsMutex = Mutex()

	@Volatile
	private var cachedTags: Set<MangaTag>? = null

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = getOrFetchTags(),
		availableStates = EnumSet.of(
			MangaState.ONGOING,
			MangaState.FINISHED,
			MangaState.PAUSED,
		),
		availableContentTypes = EnumSet.of(
			ContentType.MANGA,
			ContentType.MANHWA,
			ContentType.MANHUA,
		),
	)

	private suspend fun getOrFetchTags(): Set<MangaTag> {
		cachedTags?.let { return it }
		return tagsMutex.withLock {
			cachedTags ?: fetchTags().also { cachedTags = it }
		}
	}

	private suspend fun fetchTags(): Set<MangaTag> = runCatchingCancellable {
		webClient.httpGet("$API_BASE/front/manga?limit=0&page=0").parseJson()
			.optJSONArray("genres")
			?.mapJSONToSet { genre ->
				MangaTag(
					key = genre.getStringOrNull("id") ?: genre.getStringOrNull("_id").orEmpty(),
					title = genre.getStringOrNull("name").orEmpty(),
					source = source,
				)
			}
			?.filterTo(LinkedHashSet()) { it.key.isNotEmpty() && it.title.isNotEmpty() }
			.orEmpty()
	}.getOrDefault(emptySet())

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = if (!filter.query.isNullOrBlank()) {
			if (page > 1) return emptyList()
			"$API_BASE/front/manga/search".toHttpUrl().newBuilder()
				.addQueryParameter("query", filter.query)
				.build()
		} else {
			"$API_BASE/front/manga".toHttpUrl().newBuilder().apply {
				addQueryParameter("limit", PAGE_SIZE.toString())
				addQueryParameter("page", page.toString())
				addQueryParameter("sort", order.toApiSort())
				if (filter.tags.isNotEmpty()) {
					addQueryParameter("genre", filter.tags.joinToString(",") { it.key })
				}
				filter.types.mapNotNull { it.toApiType() }.takeIf { it.isNotEmpty() }?.let {
					addQueryParameter("type", it.joinToString(","))
				}
				filter.states.oneOrThrowIfMany()?.toApiStatus()?.let {
					addQueryParameter("status", it)
				}
			}.build()
		}
		return webClient.httpGet(url).parseJson().getJSONArray("mangas").mapJSON(::parseManga)
	}

	private fun parseManga(data: JSONObject): Manga {
		val slug = data.getString("slug")
		return Manga(
			id = generateUid(slug),
			title = data.getString("title"),
			altTitles = emptySet(),
			url = mangaUrl(slug),
			publicUrl = "$WEB_BASE/manga/$slug",
			rating = RATING_UNKNOWN,
			contentRating = null,
			coverUrl = data.getStringOrNull("coverImage")?.toImageUrl(),
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			source = source,
		)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val slug = manga.url.substringAfterLast('/').trim()
		val json = webClient.httpGet("$API_BASE/front/manga/$slug").parseJson()
		val data = json.getJSONObject("manga")
		val canonicalSlug = data.getStringOrNull("slug") ?: slug
		val chapters = json.optJSONArray("chapters")
			?.mapJSONNotNull { chapter -> parseChapter(canonicalSlug, chapter) }
			?.distinctBy { it.id }
			?.sortedBy { it.number }
			.orEmpty()

		return manga.copy(
			title = data.getStringOrNull("title") ?: manga.title,
			url = mangaUrl(canonicalSlug),
			publicUrl = "$WEB_BASE/manga/$canonicalSlug",
			coverUrl = data.getStringOrNull("coverImage")?.toImageUrl() ?: manga.coverUrl,
			description = data.getStringOrNull("synopsis") ?: manga.description,
			state = data.getStringOrNull("status").toMangaState(),
			tags = data.optJSONArray("genres")?.mapJSONToSet { genre ->
				MangaTag(
					key = genre.getStringOrNull("id") ?: genre.getStringOrNull("_id").orEmpty(),
					title = genre.getStringOrNull("name").orEmpty(),
					source = source,
				)
			}?.filterTo(LinkedHashSet()) { it.key.isNotEmpty() && it.title.isNotEmpty() }.orEmpty(),
			rating = data.optDouble("averageRating", 0.0)
				.takeIf { it > 0.0 }
				?.let { (it / 10.0).toFloat() }
				?: manga.rating,
			chapters = chapters,
		)
	}

	private fun parseChapter(slug: String, data: JSONObject): MangaChapter? {
		val numberText = data.getStringOrNull("number") ?: return null
		val number = numberText.toFloatOrNull() ?: return null
		val chapterId = data.getStringOrNull("_id") ?: "$slug/$numberText"
		return MangaChapter(
			id = generateUid(chapterId),
			title = "Chapitre $numberText",
			number = number,
			volume = 0,
			url = "$MANGA_PATH$slug/chapter/$numberText",
			scanlator = null,
			uploadDate = dateFormat.parseSafe(data.getStringOrNull("createdAt")),
			branch = null,
			source = source,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val path = chapter.url.substringAfter("manga/").trim('/')
		val slug = path.substringBefore('/')
		val number = path.substringAfterLast('/')
		val images = webClient.httpGet("$API_BASE/front/manga/$slug/chapter/$number").parseJson()
			.getJSONObject("chapter")
			.getJSONArray("images")
		return buildList(images.length()) {
			for (index in 0 until images.length()) {
				val raw = when (val image = images.opt(index)) {
					is String -> image
					is JSONObject -> image.getStringOrNull("url")
					else -> null
				}
				raw?.takeIf { it.isNotBlank() }?.toImageUrl()?.let { url ->
					add(
						MangaPage(
							id = generateUid(url),
							url = url,
							preview = null,
							source = source,
						),
					)
				}
			}
		}
	}

	private fun String.toImageUrl(): String = toAbsoluteUrl(API_HOST)

	private fun SortOrder.toApiSort() = when (this) {
		SortOrder.ALPHABETICAL -> "title"
		SortOrder.RATING, SortOrder.POPULARITY -> "rating"
		else -> "updatedAt"
	}

	private fun ContentType.toApiType() = when (this) {
		ContentType.MANGA -> "Manga"
		ContentType.MANHWA -> "Manhwa"
		ContentType.MANHUA -> "Manhua"
		else -> null
	}

	private fun MangaState.toApiStatus() = when (this) {
		MangaState.ONGOING -> "Ongoing"
		MangaState.FINISHED -> "Completed"
		MangaState.PAUSED -> "Hiatus"
		else -> null
	}

	private fun String?.toMangaState() = when (this?.lowercase(Locale.ROOT)) {
		"ongoing" -> MangaState.ONGOING
		"hiatus" -> MangaState.PAUSED
		"completed" -> MangaState.FINISHED
		else -> null
	}

	private fun mangaUrl(slug: String) = "$MANGA_PATH$slug"

	private companion object {
		const val MANGA_PATH = "/manga/"
	}
}

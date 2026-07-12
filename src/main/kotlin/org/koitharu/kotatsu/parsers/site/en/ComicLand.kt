package org.koitharu.kotatsu.parsers.site.en

import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.urlEncoded

@MangaSourceParser("COMICLAND", "ComicLand", "en", ContentType.HENTAI)
internal class ComicLand(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.COMICLAND, pageSize = 20), Interceptor {

	override val configKeyDomain = ConfigKey.Domain("comicland.org")

	override val availableSortOrders: Set<SortOrder> = setOf(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.NEWEST,
	)

	override val filterCapabilities = MangaListFilterCapabilities(
		isSearchSupported = true,
		isSearchWithFiltersSupported = false,
	)

	override fun intercept(chain: Interceptor.Chain): Response {
		return chain.proceed(
			chain.request().newBuilder()
				.header("Origin", SITE_URL)
				.header("Referer", "$SITE_URL/")
				.build(),
		)
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val query = filter.query?.trim().orEmpty()
		val endpoint = when {
			query.isNotEmpty() -> "$API_URL/comic/search?q=${query.urlEncoded()}"
			filter.tags.firstOrNull()?.key == "official" -> "$API_URL/comics/official"
			filter.tags.firstOrNull()?.key == "popular" || order == SortOrder.POPULARITY -> "$API_URL/comics/popular"
			else -> "$API_URL/comics"
		}
		val separator = if ('?' in endpoint) '&' else '?'
		val offset = (page - 1) * pageSize
		val root = JSONObject(webClient.httpGet("$endpoint${separator}offset=$offset&limit=$pageSize").body.string())
		val data = root.optJSONObject("data") ?: return emptyList()
		val items = data.optJSONArray("list") ?: data.optJSONArray("items") ?: return emptyList()
		return (0 until items.length()).mapNotNull { index ->
			items.optJSONObject(index)?.toManga()
		}
	}

	private fun JSONObject.toManga(): Manga? {
		val slug = optString("slug").takeIf { it.isNotBlank() } ?: return null
		return Manga(
			id = generateUid(slug),
			title = optString("title"),
			url = slug,
			publicUrl = "$SITE_URL/comic/$slug",
			coverUrl = optString("cover_url").takeIf { it.isNotBlank() },
			description = optString("description").takeIf { it.isNotBlank() },
			altTitles = emptySet(),
			rating = optDouble("rating", 0.0).toFloat().div(5f).takeIf { it > 0f } ?: RATING_UNKNOWN,
			tags = emptySet(),
			authors = emptySet(),
			state = null,
			contentRating = ContentRating.ADULT,
			source = source,
		)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val root = JSONObject(webClient.httpGet("$API_URL/comic/detail?slug=${manga.url}").body.string())
		val comic = root.optJSONObject("data") ?: return manga
		val slug = comic.optString("slug", manga.url)
		val chaptersJson = comic.optJSONArray("chapters")
		val chapters = chaptersJson?.mapObjects { chapter ->
			val number = chapter.optDouble("chapter_index", 0.0).toFloat()
			val numberText = number.toString().removeSuffix(".0")
			val url = "/comic/$slug/chapter/$numberText"
			MangaChapter(
				id = generateUid(url),
				title = chapter.optString("title"),
				number = number,
				volume = 0,
				url = url,
				scanlator = null,
				uploadDate = 0,
				branch = null,
				source = source,
			)
		}.orEmpty()
		return manga.copy(
			title = comic.optString("title", manga.title),
			description = comic.optString("description").takeIf { it.isNotBlank() } ?: manga.description,
			coverUrl = comic.optString("cover_url").takeIf { it.isNotBlank() } ?: manga.coverUrl,
			authors = comic.optJSONArray("authors").toNames(),
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val slug = chapter.url.substringAfter("/comic/").substringBefore("/chapter/")
		val number = chapter.url.substringAfter("/chapter/")
		val root = JSONObject(
			webClient.httpGet("$API_URL/chapter/pages_by_index?slug=$slug&index=$number").body.string(),
		)
		val pages = root.optJSONObject("data")?.optJSONArray("pages") ?: return emptyList()
		return (0 until pages.length()).mapNotNull { index ->
			pages.optString(index).takeIf { it.isNotBlank() }?.let { url ->
				MangaPage(generateUid(url), url, preview = null, source = source)
			}
		}
	}

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = setOf(
			MangaTag("recommended", "Recommended", source),
			MangaTag("official", "Official", source),
			MangaTag("popular", "Popular", source),
		),
	)

	private fun JSONArray?.toNames(): Set<String> = this?.mapObjects { it.optString("name") }
		?.filterNotTo(mutableSetOf()) { it.isBlank() }
		.orEmpty()

	private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
		return (0 until length()).mapNotNull { index -> optJSONObject(index)?.let(transform) }
	}

	private companion object {
		const val SITE_URL = "https://comicland.org"
		const val API_URL = "https://api.comicland.org/api"
	}
}

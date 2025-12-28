package org.koitharu.kotatsu.parsers.site.en

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.*

@MangaSourceParser("ATSUMOE", "Atsu.moe", "en")
internal class AtsuMoe(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.ATSUMOE, pageSize = 24) {

    override val configKeyDomain = ConfigKey.Domain("atsu.moe")
    private val apiUrl = "https://$domain/api/"

    override val availableSortOrders: Set<SortOrder> = EnumSet.of(
        SortOrder.POPULARITY,
        SortOrder.UPDATED
    )

    override val filterCapabilities: MangaListFilterCapabilities
        get() = MangaListFilterCapabilities(
            isSearchSupported = true
        )

    override suspend fun getFilterOptions(): MangaListFilterOptions {
        return MangaListFilterOptions()
    }

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val endpoint = when (order) {
            SortOrder.POPULARITY -> "infinite/trending"
            SortOrder.UPDATED -> "infinite/recentlyUpdated"
            else -> "infinite/trending"
        }

        val url = "${apiUrl}$endpoint".toHttpUrl().newBuilder()
            .addQueryParameter("page", (page - 1).toString())
            .addQueryParameter("types", "Manga,Manwha,Manhua,OEL")
            .build()

        if (!filter.query.isNullOrEmpty()) {
            return getSearchPage(page, filter.query)
        }

        val json = webClient.httpGet(url.toString()).parseJson()
        val items = json.getJSONArray("items")

        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            parseManga(item)
        }
    }

    private suspend fun getSearchPage(page: Int, query: String): List<Manga> {
        val url = "https://$domain/collections/manga/documents/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("query_by", "title,englishTitle,otherNames")
            .addQueryParameter("limit", pageSize.toString())
            .addQueryParameter("page", page.toString())
            .addQueryParameter("query_by_weights", "3,2,1")
            .addQueryParameter("include_fields", "id,title,englishTitle,poster")
            .addQueryParameter("num_typos", "4,3,2")
            .build()

        val json = webClient.httpGet(url.toString()).parseJson()
        val hits = json.getJSONArray("hits")

        return (0 until hits.length()).map { i ->
            val hit = hits.getJSONObject(i)
            val document = hit.getJSONObject("document")
            parseManga(document)
        }
    }

    private fun parseManga(json: JSONObject): Manga {
        val id = json.getString("id")
        val title = json.optString("title").ifEmpty {
            json.optString("englishTitle", "Unknown")
        }
        val poster = json.optString("poster")
        val coverUrl = if (poster.isNotEmpty()) "https://$domain$poster" else null

        return Manga(
            id = generateUid(id),
            title = title,
            altTitles = emptySet(),
            url = "/manga/$id",
            publicUrl = "https://$domain/manga/$id",
            rating = RATING_UNKNOWN,
            contentRating = ContentRating.SAFE,
            coverUrl = coverUrl,
            tags = emptySet(),
            state = null,
            authors = emptySet(),
            source = source
        )
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val mangaId = manga.url.substringAfterLast("/")
        val json = webClient.httpGet("${apiUrl}manga/page?id=$mangaId").parseJson()
        val mangaPage = json.getJSONObject("mangaPage")

        val title = mangaPage.optString("title").ifEmpty {
            mangaPage.optString("englishTitle", manga.title)
        }
        val description = mangaPage.optString("description")
        val poster = mangaPage.optString("poster")
        val coverUrl = if (poster.isNotEmpty()) "https://$domain$poster" else manga.coverUrl

        // Parse tags/genres
        val genresArray = mangaPage.optJSONArray("genres")
        val tags = if (genresArray != null) {
            (0 until genresArray.length()).map { i ->
                val genre = genresArray.getString(i)
                MangaTag(
                    key = genre.lowercase(),
                    title = genre,
                    source = source
                )
            }.toSet()
        } else emptySet<MangaTag>()

        // Parse authors
        val authorsArray = mangaPage.optJSONArray("authors")
        val authors = if (authorsArray != null) {
            (0 until authorsArray.length()).mapNotNull { i ->
                authorsArray.optString(i).takeIf { it.isNotEmpty() }
            }.toSet()
        } else emptySet<String>()

        // Parse status
        val status = mangaPage.optString("status")
        val state = when (status.lowercase()) {
            "ongoing" -> MangaState.ONGOING
            "completed" -> MangaState.FINISHED
            "hiatus" -> MangaState.PAUSED
            "cancelled" -> MangaState.ABANDONED
            else -> null
        }

        // Parse content rating
        val contentRating = when (mangaPage.optString("contentRating").lowercase()) {
            "safe" -> ContentRating.SAFE
            "suggestive" -> ContentRating.SUGGESTIVE
            "nsfw", "erotica" -> ContentRating.ADULT
            else -> ContentRating.SAFE
        }

        // Fetch chapters
        val chapters = fetchAllChapters(mangaId)

        return manga.copy(
            title = title,
            description = description,
            coverUrl = coverUrl,
            tags = tags,
            authors = authors,
            state = state,
            contentRating = contentRating,
            chapters = chapters
        )
    }

    private suspend fun fetchAllChapters(mangaId: String): List<MangaChapter> {
        val allChapters = mutableListOf<MangaChapter>()
        var currentPage = 0
        var hasMore = true

        while (hasMore) {
            val url = "${apiUrl}manga/chapters?id=$mangaId&filter=all&sort=desc&page=$currentPage"
            val json = webClient.httpGet(url).parseJson()
            val chapters = json.getJSONArray("chapters")
            val total = json.getInt("total")

            for (i in 0 until chapters.length()) {
                val chapter = chapters.getJSONObject(i)
                allChapters.add(parseChapter(chapter, mangaId))
            }

            currentPage++
            hasMore = allChapters.size < total
        }

        return allChapters
    }

    private fun parseChapter(json: JSONObject, mangaId: String): MangaChapter {
        val chapterId = json.getString("name")
        val title = json.optString("title").takeIf { it.isNotEmpty() }
        val number = json.optString("number", "0").toFloatOrNull() ?: 0f
        val volume = json.optInt("volume", 0)
        val createdAt = json.optLong("createdAt", 0L)

        return MangaChapter(
            id = generateUid("$mangaId/$chapterId"),
            title = title,
            number = number,
            volume = volume,
            url = "$mangaId/$chapterId",
            uploadDate = createdAt,
            source = source,
            scanlator = null,
            branch = null
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val (mangaId, chapterId) = chapter.url.split("/")
        val url = "${apiUrl}read/chapter".toHttpUrl().newBuilder()
            .addQueryParameter("mangaId", mangaId)
            .addQueryParameter("chapterId", chapterId)
            .build()

        val json = webClient.httpGet(url.toString()).parseJson()
        val readChapter = json.getJSONObject("readChapter")
        val pages = readChapter.getJSONArray("pages")

        return (0 until pages.length()).map { i ->
            val page = pages.getJSONObject(i)
            val pageUrl = page.getString("url")
            val fullUrl = if (pageUrl.startsWith("http")) {
                pageUrl
            } else {
                "https://$domain$pageUrl"
            }

            MangaPage(
                id = generateUid(fullUrl),
                url = fullUrl,
                preview = null,
                source = source
            )
        }
    }

    override suspend fun getRelatedManga(seed: Manga): List<Manga> {
        return emptyList()
    }
}

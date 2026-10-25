package org.koitharu.kotatsu.parsers.site.en

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNull
import java.time.Instant
import java.util.EnumSet

@MangaSourceParser("SANASCANS", "Sana Scans", "en")
internal class SanaScans(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.SANASCANS, 30) {

    override val configKeyDomain = ConfigKey.Domain("sanascans.com")
    override val availableSortOrders: Set<SortOrder> = EnumSet.of(
        SortOrder.UPDATED, SortOrder.POPULARITY, SortOrder.NEWEST, SortOrder.ALPHABETICAL,
    )
    override val filterCapabilities = MangaListFilterCapabilities(
        isSearchSupported = true, isSearchWithFiltersSupported = true, isMultipleTagsSupported = true,
    )

    private val api get() = "https://api.$domain/api"

    override suspend fun getFilterOptions(): MangaListFilterOptions {
        val genres = webClient.httpGet("$api/genres").parseJsonArray()
        return MangaListFilterOptions(
            availableTags = genres.mapJSON { MangaTag(it.getString("name").trim(), it.getInt("id").toString(), source) }
                .toSet(),
            availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED, MangaState.ABANDONED),
        )
    }

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = "$api/query".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("perPage", pageSize.toString())
            .addQueryParameter("searchTerm", filter.query.orEmpty())
            .addQueryParameter("orderBy", when (order) {
                SortOrder.POPULARITY -> "totalViews"
                SortOrder.NEWEST -> "createdAt"
                SortOrder.ALPHABETICAL -> "postTitle"
                else -> "lastChapterAddedAt"
            })
            .addQueryParameter("orderDirection", if (order == SortOrder.ALPHABETICAL) "asc" else "desc")
            .apply {
                if (filter.tags.isNotEmpty()) addQueryParameter("genreIds", filter.tags.joinToString(",") { it.key })
                filter.states.oneOrThrowIfMany()?.let { state ->
                    addQueryParameter("seriesStatus", when (state) {
                        MangaState.ONGOING -> "ONGOING"
                        MangaState.FINISHED -> "COMPLETED"
                        MangaState.ABANDONED -> "DROPPED"
                        else -> ""
                    })
                }
            }.build()
        return webClient.httpGet(url).parseJson().getJSONArray("posts").mapJSONNotNull { post ->
            if (post.optBoolean("isNovel") || post.optString("seriesType").equals("NOVEL", true)) null
            else post.toManga()
        }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val slug = manga.url.substringAfter("/series/").substringBefore('/').substringBefore('?').substringBefore('#')
        require(slug.isNotBlank()) { "Sana Scans series URL is missing" }
        val url = "$api/post".toHttpUrl().newBuilder().addQueryParameter("postSlug", slug).build()
        val data = webClient.httpGet(url).parseJson()
        val post = data.getJSONObject("post")
        if (post.optBoolean("isNovel")) throw ParseException("This source entry contains text rather than comic pages", url.toString())
        val parsed = post.toManga()
        val total = data.optInt("totalChapterCount", -1)
        val embedded = post.optJSONArray("chapters")
        val chapters = if (embedded != null && total >= 0 && embedded.length() >= total) embedded
            else loadChapters(post.getLong("id"))
        return manga.copy(
            title = parsed.title, altTitles = parsed.altTitles, coverUrl = parsed.coverUrl ?: manga.coverUrl,
            description = post.optString("postContent").takeIf { it.isNotBlank() } ?: manga.description,
            tags = parsed.tags, authors = parsed.authors, state = parsed.state,
            contentRating = if (manga.contentRating == ContentRating.ADULT) ContentRating.ADULT else parsed.contentRating,
            chapters = chapters.mapJSON { item ->
                val chapterUrl = "/series/$slug/${item.getString("slug")}"
                val number = item.optDouble("number", 0.0).toFloat().takeIf { it.isFinite() } ?: 0f
                MangaChapter(
                    id = generateUid(chapterUrl), url = chapterUrl, title = item.optString("title").takeIf { it.isNotBlank() },
                    number = number, volume = 0, scanlator = null, branch = null, source = source,
                    uploadDate = runCatching { Instant.parse(item.optString("createdAt")).toEpochMilli() }.getOrDefault(0L),
                )
            }.distinctBy { it.url }.sortedBy { it.number },
        )
    }

    private suspend fun loadChapters(postId: Long): JSONArray {
        val items = LinkedHashMap<String, JSONObject>()
        var offset = 0
        repeat(100) {
            val url = "$api/chapters".toHttpUrl().newBuilder().addQueryParameter("postId", postId.toString())
                .addQueryParameter("skip", offset.toString()).addQueryParameter("take", "200")
                .addQueryParameter("order", "asc").build()
            val response = webClient.httpGet(url).parseJson()
            val batch = response.getJSONObject("post").getJSONArray("chapters")
            val previous = items.size
            batch.mapJSON { item -> items.put(item.getString("slug"), item) }
            val total = response.optInt("totalChapterCount", -1)
            if ((total >= 0 && items.size >= total) || (total < 0 && batch.length() < 200)) {
                return JSONArray(items.values.toList())
            }
            if (batch.length() == 0 || items.size == previous) {
                throw ParseException("Sana Scans returned an incomplete repeated chapter list", url.toString())
            }
            offset += batch.length()
        }
        throw ParseException("Sana Scans chapter pagination exceeded the safe limit", "https://$domain/")
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val url = chapter.url.toAbsoluteUrl(domain)
        val doc = webClient.httpGet(url).parseHtml()
        val images = doc.select("[data-reader-page-image]")
        if (images.isEmpty()) throw ParseException("No readable images; this chapter may require unlocking on the source", url)
        return images.map { image ->
            val imageUrl = image.requireSrc()
            MangaPage(id = generateUid(imageUrl), url = imageUrl, preview = null, source = source)
        }
    }

    private fun JSONObject.toManga(): Manga {
        val url = "/series/${getString("slug")}"
        return Manga(
            id = generateUid(url), title = getString("postTitle"), url = url, publicUrl = url.toAbsoluteUrl(domain),
            altTitles = optString("alternativeTitles").split('\n').mapNotNullToSet { it.trim().takeIf(String::isNotEmpty) },
            rating = RATING_UNKNOWN, contentRating = if (optBoolean("hot")) ContentRating.ADULT else null,
            coverUrl = optString("featuredImage").takeIf(String::isNotBlank),
            tags = optJSONArray("genres")?.mapJSON { MangaTag(it.getString("name").trim(), it.getInt("id").toString(), source) }
                ?.toSet().orEmpty(),
            authors = setOfNotNull(optString("author").trim().takeIf(String::isNotEmpty)),
            state = when (optString("seriesStatus")) {
                "ONGOING" -> MangaState.ONGOING
                "COMPLETED" -> MangaState.FINISHED
                "CANCELLED", "DROPPED" -> MangaState.ABANDONED
                else -> null
            },
            source = source,
        )
    }
}

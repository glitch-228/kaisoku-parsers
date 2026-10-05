package org.koitharu.kotatsu.parsers.site.en

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.*
import java.time.Instant
import java.util.EnumSet

// Adapted from UMA's released WitchToons/VineTheme port; the live site now serves HTML/RSC rather than its old API.
@MangaSourceParser("WITCHTOONS", "WitchToons", "en")
internal class WitchToons(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.WITCHTOONS, 24) {
    override val configKeyDomain = ConfigKey.Domain("witchtoons.net")
    override val availableSortOrders = EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY, SortOrder.NEWEST, SortOrder.RATING)
    override val filterCapabilities = MangaListFilterCapabilities(isSearchSupported = true)
    override suspend fun getFilterOptions() = MangaListFilterOptions()

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val sort = when (order) {
            SortOrder.POPULARITY -> "views"
            SortOrder.NEWEST -> "newest"
            SortOrder.RATING -> "rating"
            else -> "updated"
        }
        val url = "https://$domain/series".toHttpUrl().newBuilder()
            .addQueryParameter("sort", sort).addQueryParameter("page", page.toString())
            .apply { filter.query?.takeIf(String::isNotBlank)?.let { addQueryParameter("q", it) } }.build()
        val doc = webClient.httpGet(url).parseHtml()
        val data = doc.nextJsObjects().firstOrNull { it.has("initialSeries") }
            ?: throw ParseException("Cannot find comic catalog", doc.baseUri())
        // Some sites clamp out-of-range pages to their last page. Do not append duplicates forever.
        if (data.optInt("initialPage", page) != page) return emptyList()
        return data.getJSONArray("initialSeries").mapJSON(::parseManga)
    }

    private fun parseManga(json: JSONObject): Manga {
        val slug = json.getString("slug")
        val cover = json.getStringOrNull("coverImage")?.toAbsoluteUrl(domain)
        return Manga(id = generateUid(slug), title = json.getString("title"), url = slug,
            publicUrl = "https://$domain/series/comic/$slug", coverUrl = cover, largeCoverUrl = cover,
            altTitles = json.getStringOrNull("altTitle")?.split('|')?.toSet().orEmpty(),
            rating = json.optDouble("rating", 0.0).takeIf { it > 0 }?.div(10)?.toFloat() ?: RATING_UNKNOWN,
            tags = json.optJSONArray("genres")?.mapJSONToSet { item ->
                val genre = item.optJSONObject("genre") ?: item
                val key = genre.getString("slug")
                MangaTag(genre.getStringOrNull("name") ?: key, key, source)
            }.orEmpty(),
            authors = emptySet(), state = when (json.optString("status")) {
                "ONGOING" -> MangaState.ONGOING
                "COMPLETED" -> MangaState.FINISHED
                "HIATUS" -> MangaState.PAUSED
                "CANCELLED", "DROPPED" -> MangaState.ABANDONED
                else -> null
            }, description = json.getStringOrNull("description"), source = source,
            contentRating = if (json.optBoolean("isMature")) ContentRating.ADULT else null)
    }

    private fun Document.chapterData() = nextJsObjects().firstOrNull { it.has("series") && it.has("chapters") }
        ?: throw ParseException("Cannot find chapter list", baseUri())

    override suspend fun getDetails(manga: Manga): Manga {
        val base = "https://$domain/series/comic/${manga.url}"
        val data = webClient.httpGet(base).parseHtml().chapterData()
        val all = data.getJSONArray("chapters").mapJSON { it }.toMutableList()
        val pages = data.optInt("totalPages", 1)
        if (pages !in 1..500) throw ParseException("Invalid chapter pagination", base)
        for (page in 2..pages) {
            val next = webClient.httpGet("$base?page=$page").parseHtml().chapterData()
            if (next.optInt("currentPage", page) != page) {
                throw ParseException("Chapter pagination did not advance", base)
            }
            all += next.getJSONArray("chapters").mapJSON { it }
        }
        val chapters = all.filterNot { it.optBoolean("isLocked") && !it.optBoolean("hasAccess") }
            .distinctBy { it.getString("id") }.sortedBy { it.getDouble("number") }.map { chapter ->
                val number = chapter.getDouble("number").toFloat()
                val numberText = if (number % 1f == 0f) number.toInt().toString() else number.toString()
                val url = "/series/comic/${manga.url}/chapter/$numberText"
                MangaChapter(id = generateUid(chapter.getString("id")), title = chapter.getStringOrNull("title"),
                    number = number, volume = 0, url = url, scanlator = null, branch = null, source = source,
                    uploadDate = runCatching { Instant.parse(chapter.getStringOrNull("publishedAt")).toEpochMilli() }.getOrDefault(0L))
            }
        return parseManga(data.getJSONObject("series")).copy(id = manga.id, chapters = chapters)
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
        val data = doc.nextJsObjects().firstOrNull { it.has("chapter") && it.has("isUnlocked") }
            ?: throw ParseException("Cannot find reader data", doc.baseUri())
        if (!data.optBoolean("isUnlocked")) throw ParseException("Chapter requires access on the source website", doc.baseUri())
        return data.getJSONObject("chapter").getJSONArray("pages").mapJSON { it }
            .sortedBy { it.optInt("pageNumber") }.map { page ->
            if (page.optBoolean("isEncrypted") || page.optBoolean("hasStrips") || page.optBoolean("hasFragments") ||
                page.optJSONArray("tiles")?.length()?.let { it > 0 } == true || page.optBoolean("isRedacted")) {
                throw ParseException("Unsupported protected page format", doc.baseUri())
            }
            val url = page.getString("imageUrl").toAbsoluteUrl(domain)
            MangaPage(generateUid(url), url, null, source)
        }
    }
}

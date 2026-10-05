package org.koitharu.kotatsu.parsers.site.heancms.en

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
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

@MangaSourceParser("TEMPLESCAN", "TempleScan", "en")
internal class TempleScan(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.TEMPLESCAN, pageSize = 20, searchPageSize = 10) {
    override val configKeyDomain = ConfigKey.Domain("templetoons.com")
    override val availableSortOrders = EnumSet.of(SortOrder.UPDATED, SortOrder.NEWEST,
        SortOrder.POPULARITY, SortOrder.ALPHABETICAL)
    override val filterCapabilities = MangaListFilterCapabilities(isSearchSupported = true)
    override suspend fun getFilterOptions() = MangaListFilterOptions()
    private val catalogMutex = Mutex()
    private var catalog: List<JSONObject>? = null
    private var catalogDomain: String? = null
    private var catalogTime = 0L

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(userAgentKey)
    }

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val query = filter.query
        if (!query.isNullOrBlank()) {
            val response = webClient.httpGet("https://$domain/api/search?q=${query.urlEncoded()}&page=$page&limit=10").parseJson()
            return response.getJSONArray("projects").mapJSON(::parseListingItem)
        }
        val all = getCatalog()
        val sorted = when (order) {
            SortOrder.POPULARITY -> all.sortedByDescending { it.optLong("total_views") }
            SortOrder.NEWEST -> all.sortedByDescending { it.optString("created_at") }
            SortOrder.ALPHABETICAL -> all.sortedBy { it.optString("title").lowercase() }
            else -> all.sortedByDescending { it.optString("update_chapter") }
        }
        return sorted.drop((page - 1).coerceAtLeast(0) * pageSize).take(pageSize).map(::parseListingItem)
    }

    private suspend fun getCatalog(): List<JSONObject> = catalogMutex.withLock {
        if (catalogDomain == domain && System.nanoTime() - catalogTime < 300_000_000_000L) {
            catalog?.let { return@withLock it }
        }
        val doc = webClient.httpGet("https://$domain/comics").parseHtml()
        val list = doc.nextJsObjects().firstNotNullOfOrNull { it.optJSONArray("list") }
            ?: throw ParseException("Cannot find comic catalog", doc.baseUri())
        // Infer renamed fields from rendered links instead of copying the site's rotating key.
        val cards = doc.select("div.grid a[href^=/comic/]").mapNotNull { link ->
            val title = link.selectFirst("h2")?.textOrNull() ?: link.parent()?.selectFirst("h2")?.textOrNull()
            title?.let { link.attr("href").substringAfter("/comic/").trimEnd('/') to it }
        }.toMap()
        val objects = list.mapJSON { it }
        val sample = objects.firstOrNull { obj -> obj.keys().asSequence().any { obj.optString(it) in cards } }
            ?: throw ParseException("Cannot identify comic catalog fields", doc.baseUri())
        val slugKey = sample.keys().asSequence().first { sample.optString(it) in cards }
        val titleKey = sample.keys().asSequence().firstOrNull { sample.optString(it) == cards[sample.optString(slugKey)] }
            ?: throw ParseException("Cannot identify comic titles", doc.baseUri())
        objects.forEach { it.put("series_slug", it.getString(slugKey)); it.put("title", it.getString(titleKey)) }
        catalog = objects
        catalogDomain = domain
        catalogTime = System.nanoTime()
        objects
    }

    private fun parseListingItem(json: JSONObject): Manga {
        val slug = json.getString("series_slug")
        return Manga(id = generateUid(slug), url = slug, publicUrl = "https://$domain/comic/$slug",
            title = json.getString("title"), altTitles = json.optString("alternative_names")
                .split(',', '|').mapNotNull { it.trim().takeIf(String::isNotBlank) }.toSet(),
            coverUrl = json.getStringOrNull("thumbnail"), largeCoverUrl = json.getStringOrNull("thumbnail"),
            rating = RATING_UNKNOWN, tags = emptySet(), state = parseStatus(json.getStringOrNull("status")),
            authors = emptySet(), source = source,
            contentRating = if (json.optString("badge").contains("+18")) ContentRating.ADULT else null)
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val slug = manga.url.removePrefix("/comic/").trimEnd('/')
        val doc = webClient.httpGet("https://$domain/comic/$slug").parseHtml()
        val ld = doc.select("script[type=application/ld+json]").mapNotNull {
            runCatching { JSONObject(it.data()) }.getOrNull()
        }.firstOrNull { it.optString("@type") == "ComicSeries" }
        val chapters = doc.select("ul#chapter-list a[href^=/comic/]").filterNot { link ->
            link.select("span").any { it.ownText().equals("Premium", true) }
        }.mapNotNull { link ->
            val url = link.attrAsRelativeUrl("href")
            val chapterSlug = url.substringAfterLast('/')
            val number = chapterSlug.removePrefix("chapter-").toFloatOrNull() ?: return@mapNotNull null
            MangaChapter(id = generateUid("$slug/$chapterSlug"), title = link.selectFirst("span.font-semibold")?.text(),
                number = number, volume = 0, url = url, scanlator = null, branch = null, source = source,
                uploadDate = runCatching { Instant.parse(link.selectFirst("time")?.attr("datetime")).toEpochMilli() }.getOrDefault(0L))
        }.distinctBy { it.id }.sortedBy { it.number }
        val genres = ld?.optJSONArray("genre")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }.orEmpty()
        val state = doc.select("ul[aria-label='Series stats'] li span").firstNotNullOfOrNull { parseStatus(it.text()) }
        return manga.copy(title = ld?.getStringOrNull("name") ?: manga.title,
            altTitles = setOfNotNull(ld?.getStringOrNull("alternateName")),
            description = doc.selectFirst("#series-synopsis-text")?.html() ?: ld?.getStringOrNull("description"),
            coverUrl = ld?.getStringOrNull("image") ?: manga.coverUrl,
            authors = setOfNotNull(ld?.optJSONObject("author")?.getStringOrNull("name")),
            state = state ?: manga.state, chapters = chapters,
            contentRating = if ("+18" in genres) ContentRating.ADULT else manga.contentRating,
            tags = genres.mapTo(LinkedHashSet()) { MangaTag(it, it.lowercase(), source) })
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
        val slug = chapter.url.substringAfter("/comic/").substringBefore('/')
        val arrays = doc.nextJsObjects().flatMap { obj -> obj.keys().asSequence().mapNotNull { obj.optJSONArray(it) } }
        val images = arrays.firstOrNull { arr -> arr.length() > 0 && (0 until arr.length()).all { i ->
            val url = arr.opt(i) as? String ?: return@all false
            url.startsWith("https://") && url.contains("/uploads/series/$slug/")
        } } ?: throw ParseException("Cannot find chapter images", doc.baseUri())
        return (0 until images.length()).map { images.getString(it) }.distinct().map {
            MangaPage(generateUid(it), it, null, source)
        }
    }

    private fun parseStatus(value: String?) = when (value) {
        "Ongoing" -> MangaState.ONGOING
        "Completed" -> MangaState.FINISHED
        "Dropped", "Canceled" -> MangaState.ABANDONED
        "Hiatus" -> MangaState.PAUSED
        else -> null
    }
}

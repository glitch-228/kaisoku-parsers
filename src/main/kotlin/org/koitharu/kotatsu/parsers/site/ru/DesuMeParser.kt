package org.koitharu.kotatsu.parsers.site.ru

import androidx.collection.ArrayMap
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.network.UserAgents
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.suspendlazy.getOrNull
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy
import java.util.*

@MangaSourceParser("DESUME", "Desu", "ru")
internal class DesuMeParser(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.DESUME, pageSize = 24, searchPageSize = 20) {

    override val configKeyDomain = ConfigKey.Domain(
        "desu.uno",
        "x.desu.city",
        "" +
            "desu.city",
        "desu.work",
        "desu.store",
        "desu.win",
    )

    override val availableSortOrders: Set<SortOrder> = EnumSet.of(
        SortOrder.UPDATED,
        SortOrder.POPULARITY,
        SortOrder.NEWEST,
        SortOrder.ALPHABETICAL,
    )

    override val filterCapabilities: MangaListFilterCapabilities
        get() = MangaListFilterCapabilities(
            isMultipleTagsSupported = true,
            isSearchSupported = true,
        )

    override suspend fun getFilterOptions() = MangaListFilterOptions(
        availableTags = tagsCache.get().values.toSet(),
    )

    override fun getRequestHeaders(): Headers = Headers.Builder()
        .add("User-Agent", UserAgents.KOTATSU)
        .build()

    private val tagsCache = suspendLazy(initializer = ::fetchTags)

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val query = filter.query?.trim().orEmpty()
        if (query.isNotEmpty()) {
            if (page != searchPaginator.firstPage) {
                return emptyList()
            }
            val url = "https://$domain/manga/search/".toHttpUrl().newBuilder()
                .addQueryParameter("q", query)
                .build()
            return parseSearch(webClient.httpGet(url).parseHtml())
        }
        val url = "https://$domain/manga/".toHttpUrl().newBuilder().apply {
            if (page != paginator.firstPage) {
                addQueryParameter("page", page.toString())
            }
            if (order != SortOrder.UPDATED) {
                addQueryParameter("order_by", getSortKey(order))
            }
            if (filter.tags.isNotEmpty()) {
                addQueryParameter("genres", filter.tags.joinToString(",") { it.key })
            }
        }.build()
        return parseCatalog(webClient.httpGet(url).parseHtml())
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val storedUrl = manga.url.findGroupValue(LEGACY_MANGA_URL_REGEX)?.let { "/manga/$it/" } ?: manga.url
        val doc = webClient.httpGet(storedUrl.toAbsoluteUrl(domain)).parseHtml()
        val publicUrl = doc.selectFirst("#animeView > link[itemprop=url]")
            ?.attrAsAbsoluteUrlOrNull("href") ?: storedUrl.toAbsoluteUrl(domain)
        return manga.copy(
            url = publicUrl.toRelativeUrl(domain),
            publicUrl = publicUrl,
            largeCoverUrl = doc.selectFirst("meta[property=og:image]")
                ?.attrAsAbsoluteUrlOrNull("content") ?: manga.largeCoverUrl,
            tags = doc.select(".b-entry-info a[itemprop=genre]").mapNotNullToSet { element ->
                val key = element.attr("href").substringAfter("genres=", "").takeIf(String::isNotEmpty)
                    ?: return@mapNotNullToSet null
                MangaTag(key, element.text().removePrefix("#").trim(), manga.source)
            },
            description = doc.selectFirst("#description .russian")?.textOrNull(),
            chapters = doc.select(".chlist > li").mapChapters(reversed = true) { _, item ->
                val link = item.selectFirst("h4 > a[href]") ?: return@mapChapters null
                val url = link.attrAsAbsoluteUrl("href").toRelativeUrl(domain)
                MangaChapter(
                    id = item.selectFirst("[data-chapters_id]")?.attr("data-chapters_id")
                        ?.toLongOrNull()?.let(::generateUid) ?: generateUid(url),
                    source = manga.source,
                    url = url,
                    uploadDate = 0L,
                    title = link.attrOrNull("title"),
                    volume = url.findGroupValue(VOLUME_REGEX)?.toIntOrNull() ?: 0,
                    number = url.findGroupValue(CHAPTER_REGEX)?.replace(',', '.')?.toFloatOrNull() ?: 0f,
                    scanlator = null,
                    branch = null,
                )
            },
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val fullUrl = chapter.url.toAbsoluteUrl(domain)
        val script = webClient.httpGet(fullUrl).parseHtml().select("script").firstNotNullOfOrNull { element ->
            element.data().takeIf { "window.MangaReader" in it }
        } ?: throw ParseException("Reader configuration not found", fullUrl)
        val config = script.extractMangaReaderConfig()
            ?: throw ParseException("Invalid reader configuration", fullUrl)
        val apiBaseUrl = config.getString("apiBaseUrl")
        val chapterId = config.getJSONObject("chapter").getLong("id")
        val payloadUrl = concatUrl("https://$domain", "$apiBaseUrl/chapters/$chapterId")
        val payload = webClient.httpGet(payloadUrl).parseJson()
            .getJSONObject("chapter")
            .getJSONArray("pages")
        return List(payload.length()) { index ->
            val url = payload.getJSONObject(index).getString("url")
            MangaPage(
                id = generateUid(url),
                preview = null,
                source = chapter.source,
                url = url,
            )
        }
    }

    override suspend fun resolveLink(resolver: LinkResolver, link: HttpUrl): Manga? {
        val doc = webClient.httpGet(link).parseHtml()
        val publicUrl = doc.selectFirst("#animeView > link[itemprop=url]")
            ?.attrAsAbsoluteUrlOrNull("href") ?: return null
        val mangaId = publicUrl.findGroupValue(MANGA_ID_REGEX)?.toLongOrNull() ?: return null
        val title = doc.selectFirst(".titleBar .rus-name")?.textOrNull() ?: return null
        return resolver.resolveManga(
            this,
            id = generateUid(mangaId),
            url = publicUrl.toRelativeUrl(domain),
            title = title,
        )
    }

    private suspend fun parseCatalog(doc: Document): List<Manga> {
        val tagsMap = tagsCache.getOrNull()
        return doc.select(".animeList .memberListItem").mapNotNull { item ->
            val link = item.selectFirst("a.animeTitle[href]") ?: return@mapNotNull null
            val publicUrl = link.attrAsAbsoluteUrl("href")
            val url = publicUrl.toRelativeUrl(domain)
            val mangaId = url.findGroupValue(MANGA_ID_REGEX)?.toLongOrNull() ?: return@mapNotNull null
            val originalTitle = link.text()
            val title = item.selectFirst(".dimmed.oTitle [itemprop=title]")?.text().orEmpty()
                .ifBlank { originalTitle }
            Manga(
                url = url,
                publicUrl = publicUrl,
                source = source,
                title = title,
                altTitles = setOfNotNull(originalTitle.takeIf { it != title }),
                coverUrl = item.selectFirst("a.avatar .img")?.styleValueOrNull("background-image")
                    ?.cssUrl()?.trim('\'', '"')?.toAbsoluteUrl(domain),
                state = null,
                rating = item.info("Рейтинг")?.toFloatOrNull()?.div(10f) ?: RATING_UNKNOWN,
                id = generateUid(mangaId),
                contentRating = null,
                tags = item.info("Жанры")?.split(',')?.mapNotNullToSet { genre ->
                    tagsMap?.get(genre.trim().toTitleCase())
                } ?: emptySet(),
                authors = emptySet(),
            )
        }
    }

    private fun parseSearch(doc: Document): List<Manga> {
        val row = doc.select("#acpQuickSearch tr").firstOrNull {
            it.selectFirst("th")?.text() == "Манга"
        } ?: return emptyList()
        return row.select("td li > a[href]").mapNotNull { link ->
            val publicUrl = link.attrAsAbsoluteUrl("href")
            val url = publicUrl.toRelativeUrl(domain)
            val mangaId = url.findGroupValue(MANGA_ID_REGEX)?.toLongOrNull() ?: return@mapNotNull null
            val originalTitle = link.selectFirst(".itemTitle")?.text().orEmpty()
            val title = link.selectFirst(".itemSubTitle")?.text().orEmpty().ifBlank { originalTitle }
            Manga(
                url = url,
                publicUrl = publicUrl,
                source = source,
                title = title,
                altTitles = setOfNotNull(originalTitle.takeIf { it != title }),
                coverUrl = link.selectFirst("img")?.src(),
                state = null,
                rating = RATING_UNKNOWN,
                id = generateUid(mangaId),
                contentRating = null,
                tags = emptySet(),
                authors = emptySet(),
            )
        }
    }

    private fun Element.info(key: String): String? = select(".animeInfo dl").firstOrNull {
        it.selectFirst("dt")?.text() == "$key:"
    }?.selectFirst("dd")?.textOrNull()

    private fun getSortKey(sortOrder: SortOrder) =
        when (sortOrder) {
            SortOrder.ALPHABETICAL -> "name"
            SortOrder.POPULARITY -> "popular"
            SortOrder.UPDATED -> "updated"
            SortOrder.NEWEST -> "id"
            else -> "updated"
        }

    private fun String.extractMangaReaderConfig(): JSONObject? {
        val marker = "window.MangaReader"
        val markerIndex = indexOf(marker)
        if (markerIndex < 0) return null
        val braceStart = indexOf('{', markerIndex)
        if (braceStart < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in braceStart until length) {
            val ch = this[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inString = false
                }
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return runCatching { JSONObject(substring(braceStart, i + 1)) }
                            .getOrNull()
                    }
                }
            }
        }
        return null
    }

    private suspend fun fetchTags(): Map<String, MangaTag> {
        val doc = webClient.httpGet("https://$domain/manga/").parseHtml()
        val root = doc.body().requireElementById("animeFilter")
            .selectFirstOrThrow(".catalog-genres")
        val li = root.select("li")
        val result = ArrayMap<String, MangaTag>(li.size)
        for (it in li) {
            val input = it.selectFirst("input") ?: continue
            val genreId = input.attr("data-genre-id").ifEmpty {
                it.parseFailed("data-genre-id is empty")
            }
            val genreSlug = input.attr("data-genre-slug").ifEmpty {
                it.parseFailed("data-genre-slug is empty")
            }
            val tag = MangaTag(
                source = source,
                key = "$genreId-$genreSlug",
                title = input.attr("data-genre-name").toTitleCase().ifEmpty {
                    it.parseFailed("data-genre-name is empty")
                },
            )
            result[tag.title] = tag
        }
        return result
    }

    private companion object {
        val MANGA_ID_REGEX = Regex("""\.(\d+)/?$""")
        val LEGACY_MANGA_URL_REGEX = Regex("""/manga/api/(\d+)/?$""")
        val VOLUME_REGEX = Regex("""/vol(\d+)/""")
        val CHAPTER_REGEX = Regex("""/ch([\d.,]+)/""")
    }
}

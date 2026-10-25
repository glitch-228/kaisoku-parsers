package org.koitharu.kotatsu.parsers.site.en

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.jsoup.Jsoup
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.oneOrThrowIfMany
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import java.math.BigDecimal
import java.time.Instant

/** Strapi contract adapted from the complete public Keiyoushi KappaBeast adapter. */
@MangaSourceParser("KAPPABEAST", "KappaBeast", "en")
internal class KappaBeast(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.KAPPABEAST, 20) {

    override val configKeyDomain = ConfigKey.Domain("kappabeast.com")
    private val api: String get() = "https://strapi.$domain/api"
    override val availableSortOrders = setOf(
        SortOrder.UPDATED, SortOrder.NEWEST, SortOrder.ALPHABETICAL,
    )
    override val filterCapabilities = MangaListFilterCapabilities(
        isSearchSupported = true, isSearchWithFiltersSupported = true,
    )

    override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
        .set("Referer", "https://$domain/").build()

    override suspend fun getFilterOptions() = MangaListFilterOptions(
        availableTags = listOf("Fantasy", "Romance", "Comedy", "Drama", "Thriller", "Action", "Psychological", "Isekai")
            .map { MangaTag(key = it, title = it, source = source) }.toSet(),
        availableStates = setOf(MangaState.ONGOING, MangaState.FINISHED, MangaState.PAUSED),
        availableContentTypes = setOf(ContentType.MANGA, ContentType.MANHWA, ContentType.MANHUA),
    )

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = metadataUrl().addQueryParameter("pagination[page]", page.toString())
            .addQueryParameter("pagination[pageSize]", pageSize.toString())
            .addQueryParameter("sort[0]", when (order) {
                SortOrder.NEWEST -> "createdAt:desc"
                SortOrder.ALPHABETICAL -> "title:asc"
                else -> "updatedAt:desc"
            })
        filter.query?.takeIf(String::isNotBlank)?.let { url.addQueryParameter("filters[title][\$containsi]", it) }
        filter.tags.oneOrThrowIfMany()?.let { url.addQueryParameter("filters[category][name][\$eq]", it.key) }
        filter.states.oneOrThrowIfMany()?.let {
            url.addQueryParameter("filters[manga_status][\$eq]", when (it) {
                MangaState.FINISHED -> "Completed"
                MangaState.PAUSED -> "Hiatus"
                else -> "Ongoing"
            })
        }
        filter.types.oneOrThrowIfMany()?.let {
            url.addQueryParameter("filters[type][\$eq]", it.name.lowercase().replaceFirstChar(Char::uppercase))
        }
        return webClient.httpGet(url.build()).parseJson().getJSONArray("data").mapJSON(::toManga)
    }

    private fun metadataUrl(): HttpUrl.Builder = "$api/mangas".toHttpUrl().newBuilder()
        .addQueryParameter("populate[media][populate]", "*")
        .addQueryParameter("populate[category][fields][0]", "name")

    private fun toManga(item: JSONObject): Manga {
        val slug = item.getString("slug")
        val documentId = item.getString("documentId")
        val path = "/series/$slug#$documentId"
        return Manga(
            id = generateUid(path), url = path, publicUrl = "https://$domain/series/$slug",
            title = item.getString("title"), altTitles = emptySet(), rating = RATING_UNKNOWN,
            coverUrl = item.optJSONArray("media")?.optJSONObject(0)?.optJSONObject("coverImage")
                ?.getStringOrNull("url")?.toAbsoluteUrl("strapi.$domain"),
            authors = setOfNotNull(item.getStringOrNull("author"), item.getStringOrNull("artist")),
            tags = item.optJSONArray("category")?.mapJSON { MangaTag(
                key = it.getString("name"), title = it.getString("name"), source = source,
            ) }?.toSet().orEmpty(),
            state = when (item.optString("manga_status")) {
                "Ongoing" -> MangaState.ONGOING
                "Completed" -> MangaState.FINISHED
                "Hiatus" -> MangaState.PAUSED
                else -> null
            },
            contentRating = null, description = item.getStringOrNull("description"), source = source,
        )
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val reference = manga.url.toAbsoluteUrl(domain).toHttpUrl()
        val slug = reference.pathSegments.last()
        val url = metadataUrl().addQueryParameter("filters[slug][\$eq]", slug)
            .addQueryParameter("pagination[pageSize]", "1").build()
        val item = webClient.httpGet(url).parseJson().getJSONArray("data").optJSONObject(0)
            ?: throw ParseException("Series metadata not found", url.toString())
        val documentId = item.getString("documentId")
        if (item.getString("slug") != slug || (reference.fragment != null && reference.fragment != documentId)) {
            throw ParseException("Series identity has changed", url.toString())
        }
        val current = toManga(item)
        val chapters = ArrayList<MangaChapter>()
        val seen = HashSet<String>()
        var page = 1
        do {
            currentCoroutineContext().ensureActive()
            val chapterUrl = "$api/chapters".toHttpUrl().newBuilder()
                .addQueryParameter("filters[manga][documentId][\$eq]", documentId)
                .addQueryParameter("fields[0]", "number").addQueryParameter("fields[1]", "title")
                .addQueryParameter("fields[2]", "createdAt")
                .addQueryParameter("sort[0]", "number:asc")
                .addQueryParameter("pagination[page]", page.toString())
                .addQueryParameter("pagination[pageSize]", "100").build()
            val json = webClient.httpGet(chapterUrl).parseJson()
            val rows = json.getJSONArray("data")
            for (index in 0 until rows.length()) {
                val chapter = rows.getJSONObject(index)
                val rawNumber = chapter.get("number").toString()
                if (rawNumber.length > 64) throw ParseException("Invalid chapter number", chapterUrl.toString())
                val decimal = rawNumber.toBigDecimalOrNull()?.takeIf {
                    it >= BigDecimal.ZERO && it.precision() <= 32 && it.scale() in -32..32 && it.toFloat().isFinite()
                }
                    ?: throw ParseException("Invalid chapter number", chapterUrl.toString())
                val numeric = decimal.toFloat()
                val number = decimal.stripTrailingZeros().toPlainString()
                val path = "/reader/$slug/$number#$documentId"
                if (!seen.add(path)) throw ParseException("Repeated or ambiguous chapter identity", chapterUrl.toString())
                chapters += MangaChapter(
                    id = generateUid(path), url = path, title = chapter.getStringOrNull("title"),
                    number = numeric, volume = 0, scanlator = null, branch = null,
                    uploadDate = chapter.getStringOrNull("createdAt")?.let {
                        runCatching { Instant.parse(it).toEpochMilli() }.getOrDefault(0)
                    } ?: 0, source = source,
                )
            }
            val pagination = json.getJSONObject("meta").getJSONObject("pagination")
            val pageCount = pagination.getInt("pageCount")
            if (page == 1 && rows.length() == 0 && pagination.optInt("total", -1) == 0) break
            if (pageCount !in 1..128 || chapters.size > 20_000) {
                throw ParseException("Chapter pagination limit exceeded", chapterUrl.toString())
            }
            if (page >= pageCount) break
            if (rows.length() == 0) throw ParseException("Incomplete chapter list", chapterUrl.toString())
            page++
        } while (true)
        return manga.copy(
            title = current.title, publicUrl = current.publicUrl, coverUrl = current.coverUrl ?: manga.coverUrl,
            description = current.description, authors = current.authors, tags = current.tags,
            state = current.state, chapters = chapters,
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val reference = chapter.url.toAbsoluteUrl(domain).toHttpUrl()
        val documentId = reference.fragment ?: throw ParseException("Missing series identity", chapter.url)
        val number = reference.pathSegments.last()
        val url = "$api/chapters".toHttpUrl().newBuilder()
            .addQueryParameter("filters[manga][documentId][\$eq]", documentId)
            .addQueryParameter("filters[number][\$eq]", number)
            .addQueryParameter("pagination[pageSize]", "1").build()
        val body = webClient.httpGet(url).parseJson().getJSONArray("data").optJSONObject(0)
            ?.getStringOrNull("htmlContent") ?: throw ParseException("Chapter is unavailable or locked", url.toString())
        val images = Jsoup.parseBodyFragment(body, "https://$domain/").select("div.separator > a[href]")
            .map { it.absUrl("href").toHttpUrl() }
        if (images.isEmpty() || images.size > 5_000 || images.any { !it.isHttps }) {
            throw ParseException("Chapter image URLs are missing or invalid", url.toString())
        }
        return images.map { MangaPage(id = generateUid(it.toString()), url = it.toString(), preview = null, source = source) }
    }
}

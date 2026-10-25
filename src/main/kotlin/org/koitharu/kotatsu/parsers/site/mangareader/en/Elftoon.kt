package org.koitharu.kotatsu.parsers.site.mangareader.en

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.jsoup.Jsoup
import org.koitharu.kotatsu.parsers.util.json.nextJsObjects
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.ContentRating
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
import org.koitharu.kotatsu.parsers.util.json.asTypedList
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNullToSet
import org.koitharu.kotatsu.parsers.util.mapChapters
import org.koitharu.kotatsu.parsers.util.nullIfEmpty
import org.koitharu.kotatsu.parsers.util.oneOrThrowIfMany
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.parseRaw
import org.koitharu.kotatsu.parsers.util.parseSafe
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import org.koitharu.kotatsu.parsers.util.toTitleCase
import java.text.SimpleDateFormat
import java.util.EnumSet
import java.util.Locale
import java.util.TimeZone
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

/**
 * Elf Toon moved from elftoon.com (MangaThemesia) to elftoon.net, a Next.js site (keiyoushi's VineTheme).
 * Lists come from its JSON API; details, chapters and pages from the React Server Components payload
 * of the series and chapter pages. Ids are still generated from the old MangaThemesia paths
 * (`/manga/<slug>/`, `/<slug>-chapter-<n>/`, which the site redirects) so library and history survive the move.
 */
@MangaSourceParser("ELFTOON", "Elftoon", "en")
internal class Elftoon(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.ELFTOON, 24) {

    override val configKeyDomain = ConfigKey.Domain("elftoon.net", "elftoon.com")

    private val rscHeaders = Headers.headersOf("rsc", "1")

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(userAgentKey)
    }

    override val availableSortOrders: Set<SortOrder> = EnumSet.of(
        SortOrder.UPDATED,
        SortOrder.POPULARITY,
        SortOrder.RATING,
        SortOrder.NEWEST,
    )

    override val filterCapabilities: MangaListFilterCapabilities
        get() = MangaListFilterCapabilities(
            isSearchSupported = true,
            isSearchWithFiltersSupported = true,
            isMultipleTagsSupported = true,
        )

    override suspend fun getFilterOptions() = MangaListFilterOptions(
        availableTags = fetchTags(),
        availableStates = EnumSet.of(
            MangaState.ONGOING,
            MangaState.FINISHED,
            MangaState.PAUSED,
            MangaState.ABANDONED,
            MangaState.UPCOMING,
        ),
        availableContentTypes = EnumSet.of(
            ContentType.MANHWA,
            ContentType.MANHUA,
            ContentType.MANGA,
        ),
    )

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = "https://$domain/api/series".toHttpUrl().newBuilder()
            .addQueryParameter("contentMode", "comics")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", pageSize.toString())
            .addQueryParameter(
                "sort",
                when (order) {
                    SortOrder.POPULARITY -> "views"
                    SortOrder.RATING -> "rating"
                    SortOrder.NEWEST -> "newest"
                    else -> "updated"
                },
            )
        filter.query?.trim()?.nullIfEmpty()?.let {
            url.addQueryParameter("q", it)
        }
        if (filter.tags.isNotEmpty()) {
            // Several genres are combined with AND by the site
            url.addQueryParameter("genre", filter.tags.joinToString(",") { it.key })
        }
        filter.states.oneOrThrowIfMany()?.let { state ->
            url.addQueryParameter(
                "status",
                when (state) {
                    MangaState.ONGOING -> "Ongoing"
                    MangaState.FINISHED -> "Completed"
                    MangaState.PAUSED -> "Hiatus"
                    MangaState.ABANDONED -> "Dropped"
                    MangaState.UPCOMING -> "Upcoming"
                    else -> return@let
                },
            )
        }
        filter.types.oneOrThrowIfMany()?.let { type ->
            url.addQueryParameter(
                "type",
                when (type) {
                    ContentType.MANHWA -> "MANHWA"
                    ContentType.MANHUA -> "MANHUA"
                    ContentType.MANGA -> "MANGA"
                    else -> return@let
                },
            )
        }
        val data = webClient.httpGet(url.build()).parseJson().getJSONArray("data")
        return data.mapJSON { parseManga(it) }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        // Library entries saved before the move still hold "/manga/<slug>/"
        val slug = manga.url.removeSuffix("/").substringAfterLast('/')
        val seriesUrl = "https://$domain/series/comic/$slug"
        val first = fetchSeriesPage(seriesUrl, 1)
        val series = first.getJSONObject("series")
        val chapters = first.getJSONArray("chapters").asTypedList<JSONObject>().toMutableList()
        if (chapters.size > MAX_CHAPTERS) throw ParseException("Chapter list is too large", seriesUrl)
        val totalPages = first.optInt("totalPages", 1)
        if (totalPages !in 1..MAX_CHAPTER_PAGES) throw ParseException("Invalid chapter page count", seriesUrl)
        for (page in 2..totalPages) {
            currentCoroutineContext().ensureActive()
            chapters += fetchSeriesPage(seriesUrl, page).getJSONArray("chapters").asTypedList<JSONObject>()
            if (chapters.size > MAX_CHAPTERS) throw ParseException("Chapter list is too large", seriesUrl)
        }
        val tags = series.optJSONArray("genres")?.let { parseTags(it) }
        return manga.copy(
            title = series.getStringOrNull("title") ?: manga.title,
            altTitles = buildSet {
                series.getStringOrNull("altTitle")?.let(::add)
                series.getStringOrNull("originalTitle")?.let(::add)
                series.optJSONArray("aliases")?.let { aliases ->
                    for (i in 0 until aliases.length()) {
                        aliases.optString(i).trim().nullIfEmpty()?.let(::add)
                    }
                }
                remove(series.optString("title"))
            },
            publicUrl = seriesUrl,
            coverUrl = series.getStringOrNull("coverImage")?.toAbsoluteUrl(domain) ?: manga.coverUrl,
            description = series.getStringOrNull("description")?.takeUnless { it.startsWith('$') },
            tags = if (tags.isNullOrEmpty()) manga.tags else tags,
            state = parseState(series.optString("status")) ?: manga.state,
            rating = parseRating(series),
            contentRating = contentRating(series) ?: manga.contentRating,
            chapters = parseChapters(slug, chapters),
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val chapterPath = chapter.url.removeSuffix("/")
        val url = if (chapterPath.startsWith("/series/comic/")) chapterPath.toAbsoluteUrl(domain) else {
            val split = chapterPath.removePrefix("/").split("-chapter-", limit = 2)
            if (split.size != 2 || !CHAPTER_NUMBER.matches(split[1])) {
                throw ParseException("Unrecognized saved chapter URL", chapter.url)
            }
            "https://$domain/series/comic/${split[0]}/chapter/${split[1].replace('-', '.')}"
        }
        val data = fetchRscObject(url, "\"pages\"") { it.optJSONObject("chapter")?.has("pages") == true }
            ?: throw ParseException("Chapter data not found", url)
        if (data.optBoolean("isLocked")) {
            throw ParseException("This chapter is locked and requires coins to read", url)
        }
        val declaredPages = data.getJSONObject("chapter").optJSONArray("pages")
            ?: throw ParseException("Chapter pages are missing or malformed", url)
        val pages = (0 until declaredPages.length()).map { index ->
            val page = declaredPages.optJSONObject(index)
                ?: throw ParseException("Chapter page ${index + 1} is malformed", url)
            if (page.optBoolean("isRedacted") || page.optBoolean("isLocked")) {
                throw ParseException("This chapter contains locked pages", url)
            }
            val imageUrl = (page.opt("imageUrl") as? String)?.takeIf { it.isNotBlank() }
                ?.toAbsoluteUrl(domain)?.takeIf { it.toHttpUrlOrNull() != null }
                ?: throw ParseException("Image URL for chapter page ${index + 1} is missing or invalid", url)
            MangaPage(
                id = generateUid(imageUrl),
                url = imageUrl,
                preview = null,
                source = source,
            )
        }
        if (pages.isEmpty()) {
            throw ParseException("Chapter image URLs not found", url)
        }
        return pages
    }

    private suspend fun fetchSeriesPage(seriesUrl: String, page: Int): JSONObject {
        val url = "$seriesUrl?sort=desc&page=$page"
        return fetchRscObject(url, "\"totalPages\"") { it.has("series") && it.has("chapters") }
            ?: throw ParseException("Series data not found", url)
    }

    /**
     * Requests the React Server Components payload of a page (one `<id>:<json>` row per line)
     * and returns the first object matching [predicate] in the rows containing [marker].
     */
    private suspend fun fetchRscObject(
        url: String,
        marker: String,
        predicate: (JSONObject) -> Boolean,
    ): JSONObject? {
        currentCoroutineContext().ensureActive()
        val body = webClient.httpGet(url, rscHeaders).use { response ->
            response.body.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > MAX_RSC_BYTES) {
                        throw ParseException("Reader response is too large", url)
                    }
                    output.write(buffer, 0, count)
                }
                output.toString("UTF-8")
            }
        }
        currentCoroutineContext().ensureActive()
        if (body.trimStart().startsWith('<')) {
            return Jsoup.parse(body, url).nextJsObjects().firstOrNull(predicate)
        }
        for (line in body.lineSequence()) {
            if (marker !in line) {
                continue
            }
            val payload = line.substringAfter(':', "")
            if (!payload.startsWith('{') && !payload.startsWith('[')) {
                continue
            }
            val root = runCatching { JSONTokener(payload).nextValue() }.getOrNull() ?: continue
            findObject(root, predicate)?.let { return it }
        }
        return null
    }

    private fun findObject(value: Any?, predicate: (JSONObject) -> Boolean, depth: Int = 0): JSONObject? {
        if (depth > 64) return null
        when (value) {
            is JSONObject -> {
                if (predicate(value)) {
                    return value
                }
                for (key in value.keys()) {
                    findObject(value.opt(key), predicate, depth + 1)?.let { return it }
                }
            }

            is JSONArray -> for (i in 0 until value.length()) {
                findObject(value.opt(i), predicate, depth + 1)?.let { return it }
            }
        }
        return null
    }

    private fun parseManga(json: JSONObject): Manga {
        val slug = json.getStringOrNull("urlSlug") ?: json.getString("slug")
        val url = "/manga/$slug/"
        return Manga(
            id = generateUid("/manga/$slug/"),
            title = json.getString("title"),
            altTitles = emptySet(),
            url = url,
            publicUrl = "https://$domain/series/comic/$slug",
            rating = parseRating(json),
            contentRating = contentRating(json),
            coverUrl = json.getStringOrNull("coverImage")?.toAbsoluteUrl(domain),
            tags = json.optJSONArray("genres")?.let { parseTags(it) }.orEmpty(),
            state = parseState(json.optString("status")),
            authors = emptySet(),
            source = source,
        )
    }

    private fun parseChapters(slug: String, items: List<JSONObject>): List<MangaChapter> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val ids = HashSet<String>()
        val paths = HashSet<String>()
        // A malformed/repeated pagination row must not silently replace a confirmed complete list.
        // Retain locked releases; access errors belong to the reader.
        return items.map { json ->
                val id = json.getStringOrNull("id")?.takeIf { it.isNotBlank() }
                    ?: throw ParseException("Chapter identity is missing", slug)
                if (!ids.add(id)) throw ParseException("Repeated chapter identity", slug)
                val raw = json.opt("number")?.toString().orEmpty()
                if (raw.length !in 1..64) throw ParseException("Invalid chapter number", slug)
                val number = raw.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO && it.precision() <= 32 &&
                    it.scale() in -32..32 && it.toFloat().isFinite() }
                    ?: throw ParseException("Invalid chapter number", slug)
                val numberText = number.stripTrailingZeros().toPlainString()
                val path = "/$slug-chapter-${numberText.replace('.', '-')}/"
                if (!paths.add(path)) throw ParseException("Ambiguous chapter path", slug)
                number to MangaChapter(
                    id = generateUid(path),
                    title = json.getStringOrNull("title")?.takeUnless {
                        it == numberText || it.equals("Chapter $numberText", ignoreCase = true)
                    },
                    number = number.toFloat(),
                    volume = 0,
                    url = path,
                    scanlator = null,
                    uploadDate = dateFormat.parseSafe(json.getStringOrNull("publishedAt")?.removePrefix("\$D")),
                    branch = null,
                    source = source,
                )
            }.sortedBy { it.first }.map { it.second }
    }

    private suspend fun fetchTags(): Set<MangaTag> {
        val genres = webClient.httpGet("https://$domain/api/genres").parseJson().getJSONArray("genres")
        return parseTags(genres)
    }

    private fun parseTags(genres: JSONArray): Set<MangaTag> = genres.mapJSONNotNullToSet { item ->
        // List items only carry {"genre": {"slug": ...}}
        val genre = item.optJSONObject("genre") ?: item
        val key = genre.getStringOrNull("slug") ?: return@mapJSONNotNullToSet null
        MangaTag(
            key = key,
            title = genre.getStringOrNull("name") ?: key.replace('-', ' ').toTitleCase(sourceLocale),
            source = source,
        )
    }

    private fun parseState(status: String): MangaState? = when (status.uppercase(Locale.ROOT)) {
        "ONGOING" -> MangaState.ONGOING
        "COMPLETED" -> MangaState.FINISHED
        "HIATUS" -> MangaState.PAUSED
        "DROPPED", "DISCONTINUED", "CANCELLED" -> MangaState.ABANDONED
        "UPCOMING" -> MangaState.UPCOMING
        else -> null
    }

    private fun contentRating(json: JSONObject): ContentRating? = when (json.opt("isMature")) {
        true -> ContentRating.ADULT
        false -> ContentRating.SAFE
        else -> null
    }

    private fun parseRating(json: JSONObject): Float {
        val rating = json.optDouble("rating", 0.0)
        return if (rating.isFinite() && rating > 0.0) (rating / 10.0).toFloat().coerceAtMost(1f) else RATING_UNKNOWN
    }

    private companion object {
        const val MAX_CHAPTER_PAGES = 128
        const val MAX_CHAPTERS = 20_000
        const val MAX_RSC_BYTES = 4 * 1024 * 1024
        val CHAPTER_NUMBER = Regex("""\d+(?:[.-]\d+)?""")
    }
}

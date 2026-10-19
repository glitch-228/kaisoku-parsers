package org.koitharu.kotatsu.parsers.site.madtheme.en

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.madtheme.MadthemeParser
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.nullIfEmpty
import org.koitharu.kotatsu.parsers.util.oneOrThrowIfMany
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNullToSet
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.EnumSet

/** Toonily's current Next/API frontend, adapted from UMA's MangaKParser with legacy URL identities. */
@MangaSourceParser("TOONILY_ME", "Toonily.Me", "en", ContentType.HENTAI)
internal class ToonilyMe(context: MangaLoaderContext) :
    MadthemeParser(context, MangaParserSource.TOONILY_ME, "toontop.io") {
    override val selectDesc = "div.summary div.section-body p.content"
    private val migrated: Boolean get() = domain.lowercase() in KNOWN_HOSTS
    private val site: HttpUrl get() = "https://toontop.io/".toHttpUrl()
    private val api: HttpUrl get() = "https://api.toontop.io/".toHttpUrl()

    override fun getRequestHeaders(): Headers = if (!migrated) super.getRequestHeaders() else
        super.getRequestHeaders().newBuilder().set("Referer", site.toString()).build()

    override suspend fun getFilterOptions(): MangaListFilterOptions {
        if (!migrated) return super.getFilterOptions()
        val data = webClient.httpGet(api.resolve("genres")!!).parseJson().getJSONObject("data")
        return MangaListFilterOptions(
            availableTags = data.getJSONArray("items").mapJSONNotNullToSet { item ->
                item.optString("slug").nullIfEmpty()?.let { key ->
                    MangaTag(item.getString("name").toTitleCase(sourceLocale), key, source)
                }
            },
            availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED),
        )
    }

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        if (!migrated) return super.getListPage(page, order, filter)
        val url = api.resolve("titles/search")!!.newBuilder()
            .addQueryParameter("page", page.toString()).addQueryParameter("limit", pageSize.toString())
            .addQueryParameter("sort", when (order) {
                SortOrder.POPULARITY -> "views"
                SortOrder.NEWEST -> "newest"
                SortOrder.RATING -> "rating"
                SortOrder.ALPHABETICAL -> "alphabetical"
                else -> "latest"
            })
        filter.query?.takeIf { it.isNotBlank() }?.let { url.addQueryParameter("q", it) }
        if (filter.tags.isNotEmpty()) url.addQueryParameter("genres", filter.tags.joinToString(",") { it.key })
        filter.states.oneOrThrowIfMany()?.let { state ->
            url.addQueryParameter("status", if (state == MangaState.FINISHED) "completed" else "ongoing")
        }
        return webClient.httpGet(url.build()).parseJson().getJSONObject("data").getJSONArray("items").mapJSON {
            val relative = it.getString("url")
            val public = resolveSite(relative)
            Manga(id = generateUid(relative), title = it.getString("name"), altTitles = names(it, "alt_names", "altNames"),
                url = relative, publicUrl = public, rating = rating(it),
                contentRating = contentRating(it) ?: ContentRating.ADULT, coverUrl = it.optString("cover").nullIfEmpty(),
                tags = tags(it), state = state(it), authors = names(it, "authors"), source = source,
                description = it.optString("summary").nullIfEmpty())
        }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        if (!migrated) return super.getDetails(manga)
        val requestUrl = resolveSite(manga.url)
        val doc = webClient.httpGet(requestUrl).parseHtml()
        val raw = doc.selectFirst("script#__NEXT_DATA__")?.data()
            ?: throw ParseException("Cannot find title data", requestUrl)
        val metadata = JSONObject(raw).getJSONObject("props").getJSONObject("pageProps").getJSONObject("initialManga")
        val id = metadata.getString("id").also { require(it.isNotBlank()) }
        val chaptersUrl = api.newBuilder().addPathSegment("titles").addPathSegment(id).addPathSegment("chapters")
            .addQueryParameter("cv", metadata.optLong("cv").toString()).build()
        val json = webClient.httpGet(chaptersUrl).parseJson().getJSONObject("data").getJSONArray("chapters")
        val previous = manga.chapters.orEmpty().filter { it.source == source && it.branch == null }
            .groupBy { chapter -> identityPath(chapter.url) }
        val chapters = json.mapJSON { chapter ->
            val url = chapter.getString("url")
            resolveSite(url) // Validate before accepting identities from a malformed response.
            val old = identityPath(url)?.let { key -> previous[key]?.singleOrNull() }
            val number = chapter.optDouble("number", Double.NaN).toFloat()
            if (!number.isFinite()) throw ParseException("Missing chapter number", chaptersUrl.toString())
            MangaChapter(id = old?.id ?: generateUid(url), title = chapter.optString("name").nullIfEmpty(),
                number = number, volume = old?.volume ?: 0, url = old?.url ?: url, scanlator = old?.scanlator,
                uploadDate = chapter.optString("updated_at").let(::date).takeIf { it > 0L } ?: old?.uploadDate ?: 0L, branch = old?.branch, source = source)
        }.distinctBy { it.url }.sortedBy { it.number }
        return manga.copy(title = metadata.optString("name").nullIfEmpty() ?: manga.title,
            altTitles = names(metadata, "altNames", "alt_names").ifEmpty { manga.altTitles },
            description = metadata.optString("summary").nullIfEmpty() ?: manga.description,
            tags = tags(metadata).ifEmpty { manga.tags },
            authors = (names(metadata, "authors") + names(metadata, "artists")).ifEmpty { manga.authors },
            state = state(metadata) ?: manga.state, rating = rating(metadata),
            contentRating = contentRating(metadata) ?: manga.contentRating, chapters = chapters)
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        if (!migrated) return super.getPages(chapter)
        val url = resolveSite(chapter.url)
        val raw = webClient.httpGet(url).parseHtml().selectFirst("script#__NEXT_DATA__")?.data()
            ?: throw ParseException("Cannot find chapter data", url)
        val images = JSONObject(raw).getJSONObject("props").getJSONObject("pageProps")
            .getJSONObject("initialChapter").getJSONArray("images")
        val urls = (0 until images.length()).map { i ->
            val rawImage = images.getString(i)
            if (rawImage.isBlank()) throw ParseException("Empty chapter image", url)
            val image = site.resolve(rawImage) ?: throw ParseException("Invalid chapter image", url)
            image.toString()
        }.distinct()
        if (urls.isEmpty()) throw ParseException("No chapter images", url)
        return urls.map { MangaPage(generateUid(it), it, null, source) }
    }

    override suspend fun getPageUrl(page: MangaPage): String =
        if (!migrated || '#' in page.url) super.getPageUrl(page) else page.url

    private fun resolveSite(raw: String): String {
        val absolute = raw.toHttpUrlOrNull()
        if (absolute != null && absolute.host.lowercase() !in KNOWN_HOSTS)
            throw ParseException("Unexpected title/chapter host", raw)
        val resolved = site.resolve(raw) ?: throw ParseException("Invalid title/chapter URL", raw)
        if (resolved.host.lowercase() !in KNOWN_HOSTS) throw ParseException("Unexpected title/chapter host", raw)
        return resolved.newBuilder().scheme("https").host(site.host).build().toString()
    }

    private fun identityPath(raw: String): String? {
        val absolute = raw.toHttpUrlOrNull()
        if (absolute != null && absolute.host.lowercase() !in KNOWN_HOSTS) return null
        val url = site.resolve(raw) ?: return null
        if (url.host.lowercase() !in KNOWN_HOSTS) return null
        return url.encodedPath.trimEnd('/') + (url.encodedQuery?.let { "?$it" } ?: "")
    }

    private fun names(json: JSONObject, vararg fields: String): Set<String> = fields.flatMap { key ->
        json.optJSONArray(key)?.mapJSONNotNullToSet { it.optString("name").nullIfEmpty() }.orEmpty()
    }.toSet()

    private fun tags(json: JSONObject): Set<MangaTag> = json.optJSONArray("genres")?.mapJSONNotNullToSet { tag ->
        tag.optString("slug").nullIfEmpty()?.let { key -> MangaTag(tag.getString("name").toTitleCase(sourceLocale), key, source) }
    }.orEmpty()

    private fun contentRating(json: JSONObject): ContentRating? =
        when ((json.optString("content_rating").nullIfEmpty() ?: json.optString("contentRating")).lowercase()) {
            "safe" -> ContentRating.SAFE
            "suggestive" -> ContentRating.SUGGESTIVE
            "erotica", "pornographic", "adult" -> ContentRating.ADULT
            else -> null
        }

    private fun state(json: JSONObject): MangaState? = when (json.optString("status").lowercase()) {
        "ongoing" -> MangaState.ONGOING
        "completed" -> MangaState.FINISHED
        "hiatus" -> MangaState.PAUSED
        "cancelled" -> MangaState.ABANDONED
        else -> null
    }

    private fun rating(json: JSONObject): Float {
        val value = json.optDouble("rating", Double.NaN)
        return if (value.isFinite() && value > 0) (value / 5).toFloat().coerceIn(0f, 1f) else RATING_UNKNOWN
    }

    private fun date(raw: String): Long = runCatching { Instant.parse(raw).toEpochMilli() }
        .recoverCatching { LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrDefault(0L)

    private companion object {
        val KNOWN_HOSTS = setOf("toonily.me", "www.toonily.me", "toontop.io", "www.toontop.io")
    }
}

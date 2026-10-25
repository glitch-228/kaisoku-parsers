package org.koitharu.kotatsu.site.natsu.id

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.network.UserAgents
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.*
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("IKIRU", "Ikiru", "id")
internal class Ikiru(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.IKIRU, pageSize = 21) {

    override val configKeyDomain = ConfigKey.Domain("09.ikiru.wtf", "06.ikiru.wtf", "05.ikiru.wtf")
    override val userAgentKey = ConfigKey.UserAgent(UserAgents.CHROME_DESKTOP)

    // Keep saved domain choices; only retired official hosts use the new API.
    private val apiDomain: String
        get() = when (domain) {
            "05.ikiru.wtf", "06.ikiru.wtf" -> "09.ikiru.wtf"
            else -> domain
        }

    private fun apiUrlBuilder() = "https://$apiDomain/".toHttpUrl().newBuilder()

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(userAgentKey)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
        if (request.header("User-Agent") == null) {
            builder.header("User-Agent", config[userAgentKey])
        }
        if (request.header("Referer") == null) {
            builder.header("Referer", "https://$apiDomain/")
        }
        return chain.proceed(builder.build())
    }

    override val availableSortOrders: Set<SortOrder> = EnumSet.of(
        SortOrder.POPULARITY,
        SortOrder.UPDATED,
        SortOrder.RATING,
        SortOrder.ALPHABETICAL,
        SortOrder.ALPHABETICAL_DESC,
    )

    override val filterCapabilities: MangaListFilterCapabilities
        get() = MangaListFilterCapabilities(
            isSearchSupported = true,
            isSearchWithFiltersSupported = true,
            isMultipleTagsSupported = true,
            isTagsExclusionSupported = false,
            isAuthorSearchSupported = false,
        )

    private val availableTags = suspendLazy(initializer = ::fetchTags)

    private suspend fun fetchTags(): Set<MangaTag> {
        val url = apiUrlBuilder().apply {
            addPathSegment("api")
            addPathSegment("user")
            addPathSegment("genres")
        }.build()
        val response = webClient.httpGet(url)
        val json = response.parseJson()
        val allGenres = json.getJSONObject("data").getJSONArray("allGenres")
        return allGenres.mapJSONNotNullToSet { item ->
            val id = item.getStringOrNull("id") ?: return@mapJSONNotNullToSet null
            val name = item.getStringOrNull("name") ?: id
            MangaTag(
                key = id,
                title = name,
                source = source,
            )
        }
    }

    override suspend fun getFilterOptions(): MangaListFilterOptions {
        return MangaListFilterOptions(
            availableTags = availableTags.get(),
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
    }

    override suspend fun getListPage(
        page: Int,
        order: SortOrder,
        filter: MangaListFilter,
    ): List<Manga> {
        val url = apiUrlBuilder().apply {
            addPathSegment("api")
            addPathSegment("public")
            addPathSegment("library")
            addPathSegment("search")

            addQueryParameter("page", page.toString())

            if (!filter.query.isNullOrEmpty()) {
                addQueryParameter("query", filter.query)
            }

            for ((_, key) in filter.tags) {
                addQueryParameter("genre", key)
            }

            val contentType = filter.types.oneOrThrowIfMany()
            if (contentType != null) {
                val typeParam = when (contentType) {
                    ContentType.MANGA -> "MANGA"
                    ContentType.MANHWA -> "MANHWA"
                    ContentType.MANHUA -> "MANHUA"
                    else -> null
                }
                if (typeParam != null) {
                    addQueryParameter("type", typeParam)
                }
            }

            val state = filter.states.oneOrThrowIfMany()
            if (state != null) {
                val statusParam = when (state) {
                    MangaState.ONGOING -> "ONGOING"
                    MangaState.FINISHED -> "COMPLETED"
                    MangaState.PAUSED -> "HIATUS"
                    else -> null
                }
                if (statusParam != null) {
                    addQueryParameter("status", statusParam)
                }
            }

            val (sortBy, sortDir) = when (order) {
                SortOrder.POPULARITY -> "popular" to "desc"
                SortOrder.UPDATED -> "updated" to "desc"
                SortOrder.RATING -> "rating" to "desc"
                SortOrder.ALPHABETICAL -> "title" to "asc"
                SortOrder.ALPHABETICAL_DESC -> "title" to "desc"
                else -> "popular" to "desc"
            }
            addQueryParameter("sortBy", sortBy)
            addQueryParameter("sort", sortDir)
        }.build()

        val response = webClient.httpGet(url)
        val json = response.parseJson()
        val mangasArray = json.getJSONObject("data").getJSONArray("mangas")
        require(mangasArray.length() <= 1000) { "Ikiru listing exceeds its safe limit" }

        return mangasArray.mapJSONNotNull { item ->
            val slug = item.getStringOrNull("slug") ?: return@mapJSONNotNull null
            val relativeUrl = "/manga/$slug"
            val metadata = item.optJSONObject("metadata")
            val score = metadata?.getDoubleOrDefault("score", -1.0) ?: -1.0
            val rating = if (score > 0) (score / 10.0).toFloat() else RATING_UNKNOWN

            val tags = metadata?.optJSONArray("genre")?.mapJSONNotNullToSet { g ->
                val key = g.getStringOrNull("id") ?: return@mapJSONNotNullToSet null
                val name = g.getStringOrNull("name") ?: key
                MangaTag(key = key, title = name, source = source)
            }.orEmpty()

            Manga(
                id = generateUid(relativeUrl),
                title = item.getStringOrNull("title") ?: slug,
                altTitles = emptySet(),
                url = relativeUrl,
                publicUrl = relativeUrl.toAbsoluteUrl(apiDomain),
                rating = rating,
                contentRating = null,
                coverUrl = item.getStringOrNull("featuredImage"),
                tags = tags,
                state = null,
                authors = emptySet(),
                source = source,
            )
        }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val slug = titleSlug(manga.url)
        val url = apiUrlBuilder().apply {
            addPathSegment("api")
            addPathSegment("public")
            addPathSegment("manga")
            addPathSegment(slug)
        }.build()

        val response = webClient.httpGet(url)
        val json = response.parseJson()
        val data = json.getJSONObject("data")

        val metadata = data.optJSONObject("metadata")
        val score = metadata?.getDoubleOrDefault("score", -1.0) ?: -1.0
        val rating = if (score > 0) (score / 10.0).toFloat() else manga.rating

        val altTitles = metadata?.optJSONArray("alternateTitles")?.toStringSet().orEmpty()

        val tags = metadata?.optJSONArray("genre")?.mapJSONNotNullToSet { g ->
            val key = g.getStringOrNull("id") ?: return@mapJSONNotNullToSet null
            val name = g.getStringOrNull("name") ?: key
            MangaTag(key = key, title = name, source = source)
        }.takeUnless { it.isNullOrEmpty() } ?: manga.tags

        val authors = (
            metadata?.optJSONArray("author")?.mapJSONNotNullToSet { it.getStringOrNull("name") }.orEmpty() +
            metadata?.optJSONArray("artist")?.mapJSONNotNullToSet { it.getStringOrNull("name") }.orEmpty()
        )

        val state = when (data.getStringOrNull("status")?.uppercase()) {
            "ONGOING" -> MangaState.ONGOING
            "COMPLETED" -> MangaState.FINISHED
            "HIATUS" -> MangaState.PAUSED
            else -> null
        }

        val contentRating = when {
            data.optBoolean("isAdult") -> ContentRating.ADULT
            manga.contentRating == ContentRating.ADULT -> ContentRating.ADULT
            data.has("isAdult") && !data.isNull("isAdult") -> ContentRating.SAFE
            else -> manga.contentRating
        }

        val rawDescription = data.getStringOrNull("description")
        val description = rawDescription?.let {
            if (it.contains('<') && it.contains('>')) {
                Jsoup.parse(it).text().nullIfEmpty()
            } else {
                it
            }
        }

        val mangaTitle = data.getStringOrNull("title") ?: manga.title

        val chaptersArray = data.getJSONObject("chapters").getJSONArray("chapters")
        require(chaptersArray.length() <= 20_000) { "Ikiru chapter list exceeds its safe limit" }
        val chapters = chaptersArray.mapChapters(reversed = true) { _, ch ->
            val number = ch.getDouble("number").toFloat()
            require(number.isFinite() && number >= 0) { "Invalid Ikiru chapter number" }
            val numberStr = ch.getStringOrNull("number") ?: if (number % 1.0f == 0.0f) number.toInt().toString() else number.toString()
            val relUrl = "/manga/$slug/chapter-$numberStr"
            val dateStr = ch.getStringOrNull("updatedAt") ?: ch.getStringOrNull("createdAt")
            val uploadDate = parseDate(dateStr)

            MangaChapter(
                id = generateUid(relUrl),
                url = relUrl,
                title = "Chapter $numberStr",
                number = number,
                volume = 0,
                uploadDate = uploadDate,
                scanlator = null,
                branch = null,
                source = source,
            )
        }

        return manga.copy(
            title = mangaTitle,
            altTitles = altTitles,
            rating = rating,
            contentRating = contentRating,
            coverUrl = data.getStringOrNull("featuredImage") ?: manga.coverUrl,
            description = description,
            tags = tags,
            state = state,
            authors = authors,
            chapters = chapters,
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val mangaSlug = titleSlug(chapter.url)
        val chapterNumber = chapter.url.substringAfterLast("/chapter-").substringBefore('?').trimEnd('/')
        require(chapterNumber.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true) {
            "Invalid saved Ikiru chapter reference"
        }
        val url = apiUrlBuilder().apply {
            addPathSegment("api")
            addPathSegment("public")
            addPathSegment("chapter")
            addPathSegment(chapterNumber)
            addQueryParameter("mangaSlug", mangaSlug)
        }.build()

        val response = webClient.httpGet(url)
        val json = response.parseJson()
        val data = json.getJSONObject("data")
        val medias = data.getJSONArray("medias")
        require(medias.length() in 1..5000) { "Ikiru returned an empty or oversized page list" }

        return medias.mapJSONIndexed { index, media ->
            val filePath = media.getString("filePath").toAbsoluteUrl(apiDomain)
            require(filePath.toHttpUrl().isHttps) { "Invalid Ikiru image URL" }
            val pageNumber = media.getIntOrDefault("pageNumber", index)
            MangaPage(
                id = generateUid("${chapter.url}#$pageNumber"),
                url = filePath,
                preview = null,
                source = source,
            )
        }
    }

    override suspend fun resolveLink(resolver: LinkResolver, link: HttpUrl): Manga? {
        val segments = link.pathSegments
        if (segments.size >= 2 && segments[0] == "manga") {
            val slug = segments[1]
            val relativeUrl = "/manga/$slug"
            return getDetails(
                Manga(
                    id = generateUid(relativeUrl),
                    title = slug,
                    altTitles = emptySet(),
                    url = relativeUrl,
                    publicUrl = relativeUrl.toAbsoluteUrl(apiDomain),
                    rating = RATING_UNKNOWN,
                    contentRating = null,
                    coverUrl = null,
                    tags = emptySet(),
                    state = null,
                    authors = emptySet(),
                    source = source,
                )
            )
        }
        return null
    }

    private fun titleSlug(url: String): String {
        val segments = url.toAbsoluteUrl(apiDomain).toHttpUrl().pathSegments.filter(String::isNotEmpty)
        require(segments.size >= 2 && segments[0] == "manga" && segments[1].isNotBlank()) {
            "Invalid saved Ikiru title reference"
        }
        return segments[1]
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return 0L
        val fullFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val parsed = fullFormat.parseSafe(dateStr)
        if (parsed != 0L) return parsed
        val shortFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return shortFormat.parseSafe(dateStr)
    }
}

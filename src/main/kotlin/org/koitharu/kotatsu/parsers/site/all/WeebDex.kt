package org.koitharu.kotatsu.parsers.site.all

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.text.SimpleDateFormat
import java.util.*

internal abstract class WeebDexParser(
    context: MangaLoaderContext,
    source: MangaParserSource,
    private val lang: String,
) : PagedMangaParser(context, source, pageSize = 25) {

    override val configKeyDomain = ConfigKey.Domain("weebdex.org")
    private val apiUrl = "https://api.weebdex.org/api/v1"

    override val availableSortOrders: Set<SortOrder> = EnumSet.of(
        SortOrder.UPDATED,
        SortOrder.POPULARITY,
        SortOrder.NEWEST,
        SortOrder.ALPHABETICAL
    )

    override val filterCapabilities: MangaListFilterCapabilities
        get() = MangaListFilterCapabilities(
            isSearchSupported = true,
            isSearchWithFiltersSupported = true,
            isMultipleTagsSupported = true,
            isTagsExclusionSupported = false
        )

    override suspend fun getFilterOptions(): MangaListFilterOptions {
        val tagsJson = webClient.httpGet("$apiUrl/tags").parseJson().getJSONArray("data")
        val tags = (0 until tagsJson.length()).map { i ->
            val tag = tagsJson.getJSONObject(i)
            MangaTag(
                key = tag.getString("id"),
                title = tag.getString("name"),
                source = source
            )
        }.toSet()

        return MangaListFilterOptions(
            availableTags = tags,
            availableStates = EnumSet.of(
                MangaState.ONGOING,
                MangaState.FINISHED
            ),
            availableContentRating = EnumSet.of(
                ContentRating.SAFE,
                ContentRating.SUGGESTIVE,
                ContentRating.ADULT
            )
        )
    }

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = "$apiUrl/manga".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("lang", lang)
            .addQueryParameter("hasChapters", "1")

        // Sorting
        when (order) {
            SortOrder.UPDATED -> {
                url.addQueryParameter("sort", "updatedAt")
                url.addQueryParameter("order", "desc")
            }
            SortOrder.POPULARITY -> {
                url.addQueryParameter("sort", "views")
                url.addQueryParameter("order", "desc")
            }
            SortOrder.NEWEST -> {
                url.addQueryParameter("sort", "createdAt")
                url.addQueryParameter("order", "desc")
            }
            SortOrder.ALPHABETICAL -> {
                url.addQueryParameter("sort", "title")
                url.addQueryParameter("order", "asc")
            }
            else -> {}
        }

        // Search query
        if (!filter.query.isNullOrEmpty()) {
            url.addQueryParameter("title", filter.query)
        }

        // Tags
        if (filter.tags.isNotEmpty()) {
            filter.tags.forEach { tag ->
                url.addQueryParameter("tag", tag.key)
            }
        }

        // State
        filter.states.oneOrThrowIfMany()?.let { state ->
            when (state) {
                MangaState.ONGOING -> url.addQueryParameter("status", "ongoing")
                MangaState.FINISHED -> url.addQueryParameter("status", "completed")
                else -> {}
            }
        }

        // Content rating
        filter.contentRating.oneOrThrowIfMany()?.let { rating ->
            when (rating) {
                ContentRating.SAFE -> url.addQueryParameter("contentRating", "safe")
                ContentRating.SUGGESTIVE -> url.addQueryParameter("contentRating", "suggestive")
                ContentRating.ADULT -> url.addQueryParameter("contentRating", "nsfw")
                else -> {}
            }
        }

        val json = webClient.httpGet(url.build().toString()).parseJson()
        val data = json.getJSONArray("data")
        val hasNextPage = !json.optBoolean("lastPage", true)

        return (0 until data.length()).map { i ->
            val item = data.getJSONObject(i)
            parseManga(item)
        }
    }

    private fun parseManga(json: JSONObject): Manga {
        val id = json.getString("id")
        val title = json.getString("title")
        val coverUrl = json.optString("coverUrl").takeIf { it.isNotEmpty() }

        return Manga(
            id = generateUid(id),
            url = "/manga/$id",
            publicUrl = "https://$domain/manga/$id",
            coverUrl = coverUrl,
            title = title,
            altTitles = emptySet(),
            rating = RATING_UNKNOWN,
            tags = emptySet(),
            authors = emptySet(),
            state = null,
            source = source,
            contentRating = ContentRating.SAFE
        )
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val json = webClient.httpGet("$apiUrl${manga.url}").parseJson()

        val description = json.optString("description")
        val statusStr = json.optString("status")
        val state = when (statusStr.lowercase()) {
            "ongoing" -> MangaState.ONGOING
            "completed" -> MangaState.FINISHED
            "hiatus" -> MangaState.PAUSED
            "cancelled" -> MangaState.ABANDONED
            else -> null
        }

        val tagsArray = json.optJSONArray("tags")
        val tags = if (tagsArray != null) {
            (0 until tagsArray.length()).mapNotNull { i ->
                val tagJson = tagsArray.getJSONObject(i)
                MangaTag(
                    key = tagJson.getString("id"),
                    title = tagJson.getString("name"),
                    source = source
                )
            }.toSet()
        } else emptySet()

        val authorsArray = json.optJSONArray("authors")
        val authors = if (authorsArray != null) {
            (0 until authorsArray.length()).map { i ->
                authorsArray.getString(i)
            }.toSet()
        } else emptySet()

        val contentRating = when (json.optString("contentRating").lowercase()) {
            "safe" -> ContentRating.SAFE
            "suggestive" -> ContentRating.SUGGESTIVE
            "nsfw" -> ContentRating.ADULT
            else -> ContentRating.SAFE
        }

        // Get chapters
        val chaptersJson = webClient.httpGet("$apiUrl${manga.url}/chapters?lang=$lang&order=desc").parseJson()
        val chapters = parseChapterList(chaptersJson, manga)

        return manga.copy(
            description = description,
            state = state,
            tags = tags,
            authors = authors,
            contentRating = contentRating,
            chapters = chapters
        )
    }

    private fun parseChapterList(json: JSONObject, manga: Manga): List<MangaChapter> {
        val chapters = mutableListOf<MangaChapter>()
        val data = json.getJSONArray("data")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        for (i in 0 until data.length()) {
            val chapter = data.getJSONObject(i)
            val chapterId = chapter.getString("id")
            val chapterNumber = chapter.optDouble("chapter", 0.0).toFloat()
            val volumeNumber = chapter.optInt("volume", 0)
            val title = chapter.optString("title")
            val uploadDate = chapter.optString("createdAt")
            val scanlator = chapter.optString("scanlator").takeIf { it.isNotEmpty() }

            val date = try {
                dateFormat.parse(uploadDate)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }

            chapters.add(
                MangaChapter(
                    id = generateUid(chapterId),
                    title = title,
                    number = chapterNumber,
                    volume = volumeNumber,
                    url = "/manga/${manga.url.substringAfterLast("/")}/chapters/$chapterId",
                    uploadDate = date,
                    source = source,
                    scanlator = scanlator,
                    branch = null
                )
            )
        }

        return chapters
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val json = webClient.httpGet("$apiUrl${chapter.url}").parseJson()
        val pagesArray = json.getJSONArray("pages")

        return (0 until pagesArray.length()).map { i ->
            val pageUrl = pagesArray.getString(i)
            MangaPage(
                id = generateUid("$pageUrl-$i"),
                url = pageUrl,
                preview = null,
                source = source
            )
        }
    }

    @MangaSourceParser("WEEBDEX_EN", "WeebDex (English)", "en")
    class English(context: MangaLoaderContext) : WeebDexParser(
        context,
        MangaParserSource.WEEBDEX_EN,
        "en"
    )

    @MangaSourceParser("WEEBDEX_ES", "WeebDex (Español)", "es")
    class Spanish(context: MangaLoaderContext) : WeebDexParser(
        context,
        MangaParserSource.WEEBDEX_ES,
        "es"
    )

    @MangaSourceParser("WEEBDEX_FR", "WeebDex (Français)", "fr")
    class French(context: MangaLoaderContext) : WeebDexParser(
        context,
        MangaParserSource.WEEBDEX_FR,
        "fr"
    )

    @MangaSourceParser("WEEBDEX_PT", "WeebDex (Português)", "pt")
    class Portuguese(context: MangaLoaderContext) : WeebDexParser(
        context,
        MangaParserSource.WEEBDEX_PT,
        "pt"
    )

    @MangaSourceParser("WEEBDEX_DE", "WeebDex (Deutsch)", "de")
    class German(context: MangaLoaderContext) : WeebDexParser(
        context,
        MangaParserSource.WEEBDEX_DE,
        "de"
    )

    @MangaSourceParser("WEEBDEX_IT", "WeebDex (Italiano)", "it")
    class Italian(context: MangaLoaderContext) : WeebDexParser(
        context,
        MangaParserSource.WEEBDEX_IT,
        "it"
    )

    @MangaSourceParser("WEEBDEX_RU", "WeebDex (Русский)", "ru")
    class Russian(context: MangaLoaderContext) : WeebDexParser(
        context,
        MangaParserSource.WEEBDEX_RU,
        "ru"
    )

    @MangaSourceParser("WEEBDEX_JA", "WeebDex (日本語)", "ja")
    class Japanese(context: MangaLoaderContext) : WeebDexParser(
        context,
        MangaParserSource.WEEBDEX_JA,
        "ja"
    )

    @MangaSourceParser("WEEBDEX_ZH", "WeebDex (中文)", "zh")
    class Chinese(context: MangaLoaderContext) : WeebDexParser(
        context,
        MangaParserSource.WEEBDEX_ZH,
        "zh"
    )

    @MangaSourceParser("WEEBDEX_KO", "WeebDex (한국어)", "ko")
    class Korean(context: MangaLoaderContext) : WeebDexParser(
        context,
        MangaParserSource.WEEBDEX_KO,
        "ko"
    )
}

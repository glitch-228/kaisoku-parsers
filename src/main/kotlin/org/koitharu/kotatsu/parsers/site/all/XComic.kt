package org.koitharu.kotatsu.parsers.site.all

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.GraphQLException
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import java.util.EnumSet
import java.util.Locale

/** Released UMA GraphQL contracts, with title browsing and lazy edition selection. */
@MangaSourceParser("XCOMIC", "XCOMIC")
internal class XComic(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.XCOMIC, pageSize = 30) {

    override val configKeyDomain = ConfigKey.Domain("xcomic.me")
    override val availableSortOrders: Set<SortOrder> = EnumSet.of(
        SortOrder.UPDATED, SortOrder.POPULARITY, SortOrder.RATING, SortOrder.ALPHABETICAL,
    )
    override val filterCapabilities = MangaListFilterCapabilities(
        isSearchSupported = true, isSearchWithFiltersSupported = true,
        isMultipleTagsSupported = true, isTagsExclusionSupported = true,
    )

    override suspend fun getFilterOptions() = MangaListFilterOptions(
        availableTags = GENRES.mapTo(LinkedHashSet()) { (name, key) -> MangaTag(name, key, source) },
        availableLocales = LANGUAGES.keys.mapTo(LinkedHashSet()) { locale(it) },
        availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED, MangaState.PAUSED, MangaState.ABANDONED),
    )

    override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
        .set("Referer", "https://$domain/").set("Origin", "https://$domain").build()

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val lang = languageCode(filter.locale)
        val select = JSONObject().put("word", filter.query.orEmpty()).put("page", page)
            .put("size", pageSize).put("init", (page - 1) * pageSize).put("where", "browse")
            .put("sortby", when (order) {
                SortOrder.POPULARITY, SortOrder.RATING -> "field_score"
                SortOrder.ALPHABETICAL -> "field_name_asc"
                else -> "field_update"
            })
            .put("incTLangs", JSONArray(listOfNotNull(lang)))
            .put("incGenres", JSONArray(filter.tags.map { it.key }))
            .put("excGenres", JSONArray(filter.tagsExclude.map { it.key }))
            .put("origStatus", JSONArray(filter.states.mapNotNull { it.toApiStatus() }))
            .put("incContentRatings", JSONArray(filter.contentRating.map { when (it) {
                ContentRating.SAFE -> "safe"
                ContentRating.SUGGESTIVE -> "suggestive"
                ContentRating.ADULT -> "explicit"
            } }))
        val items = query(BROWSE, JSONObject().put("select", select)).getJSONArray("get_title_browse_items")
        return List(items.length()) { index ->
            val node = items.getJSONObject(index)
            val data = node.getJSONObject("data")
            val id = node.getString("id")
            // Locale is part of the edition choice; details retain this title identity.
            val url = id + (lang?.let { ":$it" } ?: "")
            Manga(
                id = generateUid(url), url = url, publicUrl = "https://$domain/title/$id",
                title = data.getString("title"), altTitles = setOfNotNull(data.text("native_title")),
                coverUrl = (data.text("cover_local_url") ?: data.text("cover_url"))?.toAbsoluteUrl(domain),
                rating = RATING_UNKNOWN, contentRating = contentRating(data.text("content_rating_id")),
                tags = emptySet(), authors = emptySet(), state = null, source = source,
            )
        }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val parts = manga.url.split(':', limit = 3)
        val id = parts[0]
        val language = parts.getOrNull(1)?.ifEmpty { null }
        val title = query(TITLE, JSONObject().put("id", id)).getJSONObject("get_title_titleNode")
            .getJSONObject("data")
        val edition = parts.getOrNull(2)?.ifEmpty { null } ?: selectEdition(
            webClient.httpGet("https://$domain/title/$id").parseHtml(), language,
        )
            ?: throw ParseException("No readable edition in the selected language", manga.publicUrl)
        val comic = query(COMIC, JSONObject().put("id", edition)).getJSONObject("get_comicNode")
            .getJSONObject("data")
        if (!comic.optBoolean("isPublic", true) || comic.optString("dbStatus") !in listOf("", "normal")) {
            throw ParseException("The selected edition is unavailable", manga.publicUrl)
        }
        val actualLanguage = comic.text("translatedLanguage")
        if (language != null && actualLanguage != language) {
            throw ParseException("The selected edition has changed language", manga.publicUrl)
        }
        return manga.copy(
            // Preserve the initial title ID and pin its edition for future library updates.
            url = "$id:${language.orEmpty()}:$edition",
            title = title.text("title") ?: manga.title,
            altTitles = title.strings("alt_titles").toSet(),
            authors = (title.strings("authors") + title.strings("artists")).toSet(),
            description = comic.optJSONObject("summary")?.text("text") ?: title.text("description"),
            tags = title.strings("genre_ids").mapTo(LinkedHashSet()) { key ->
                MangaTag(GENRES.firstOrNull { it.second == key }?.first ?: key.replace('_', ' '), key, source)
            },
            rating = title.optDouble("vote_avg", Double.NaN).let {
                if (it.isFinite()) (it.toFloat() / 5f).coerceIn(0f, 1f) else manga.rating
            },
            contentRating = contentRating(comic.text("contentRating") ?: title.text("content_rating_id")),
            state = when (comic.optString("originalStatus").lowercase(Locale.ROOT)) {
                "ongoing" -> MangaState.ONGOING
                "completed" -> MangaState.FINISHED
                "hiatus" -> MangaState.PAUSED
                "cancelled", "canceled" -> MangaState.ABANDONED
                else -> manga.state
            },
            chapters = chapters(edition),
        )
    }

    private fun selectEdition(doc: Document, language: String?): String? {
        val flags = language?.let { LANGUAGES[it] } ?: LANGUAGES.getValue("en")
        data class Edition(val id: String, val count: Int, val preferred: Boolean)
        val editions = doc.select("a[href^=/source/]").mapNotNull { link ->
            val card = link.parents().firstOrNull { parent ->
                parent.hasClass("rounded-box") && parent.select("a[href^=/source/]").size == 1
            } ?: return@mapNotNull null
            val count = CHAPTER_COUNT.find(card.text())?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
                ?: return@mapNotNull null
            if (count == 0) return@mapNotNull null
            val flag = link.parent()?.selectFirst(".font-family-NotoColorEmoji")?.text().orEmpty()
            val preferred = flag in flags
            if (language != null && !preferred) return@mapNotNull null
            Edition(link.attr("href").substringAfter("/source/").substringBefore('/'), count, preferred)
        }
        return editions.maxWithOrNull(compareBy<Edition> { it.preferred }.thenBy { it.count })?.id
    }

    private suspend fun chapters(comicId: String): List<MangaChapter> {
        val result = LinkedHashMap<String, MangaChapter>()
        var page = 1
        while (true) {
            val select = JSONObject().put("comic_id", comicId).put("page", page).put("size", 100)
                .put("sortby", "chapter_desc")
            val root = query(CHAPTERS, JSONObject().put("select", select)).getJSONObject("get_comic_chapterList_fullList")
            val items = root.getJSONArray("items")
            var added = 0
            for (index in 0 until items.length()) {
                val node = items.getJSONObject(index)
                val data = node.getJSONObject("data")
                val id = data.text("id") ?: node.getString("id")
                if (id in result) continue
                val number = data.optDouble("chaNum", data.optDouble("serial", 0.0)).toFloat()
                result[id] = MangaChapter(
                    id = generateUid(id), url = id, title = data.text("dname") ?: "Chapter $number",
                    number = number, volume = data.optInt("volNum"), scanlator = data.text("srcName"),
                    uploadDate = data.optLong("datePublic"), branch = null, source = source,
                )
                added++
            }
            val next = root.optJSONObject("paging")?.optInt("next") ?: 0
            if (next == 0) break
            if (added == 0 || next <= page || next > 1000) {
                throw ParseException("Chapter pagination did not advance", "https://$domain/source/$comicId")
            }
            page = next
        }
        return result.values.sortedWith(compareBy<MangaChapter> { it.volume }.thenBy { it.number })
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val data = query(PAGES, JSONObject().put("id", chapter.url)).getJSONObject("get_chapterNode")
            .getJSONObject("data")
        return data.strings("imageUrls").map { image ->
            val url = image.toAbsoluteUrl(domain)
            MangaPage(id = generateUid(url), url = url, preview = null, source = source)
        }
    }

    private suspend fun query(query: String, variables: JSONObject): JSONObject {
        val root = webClient.httpPost("https://$domain/query/".toHttpUrl(),
            JSONObject().put("query", query).put("variables", variables)).parseJson()
        root.optJSONArray("errors")?.takeIf { it.length() > 0 }?.let { throw GraphQLException(it) }
        return root.getJSONObject("data")
    }

    private fun JSONObject.text(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    private fun JSONObject.strings(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            if (array.isNull(i)) null else array.optString(i).takeIf { it.isNotBlank() }
        }
    }
    private fun contentRating(value: String?): ContentRating? = when (value?.lowercase(Locale.ROOT)) {
        "explicit", "adult" -> ContentRating.ADULT
        "suggestive", "mature" -> ContentRating.SUGGESTIVE
        "safe", "general" -> ContentRating.SAFE
        else -> null
    }
    private fun MangaState.toApiStatus(): String? = when (this) {
        MangaState.ONGOING -> "ongoing"
        MangaState.FINISHED -> "completed"
        MangaState.PAUSED -> "hiatus"
        MangaState.ABANDONED -> "cancelled"
        else -> null
    }
    private fun locale(code: String): Locale = when (code) {
        "pt_br" -> Locale.forLanguageTag("pt-BR")
        "es_419" -> Locale.forLanguageTag("es-419")
        "zh_hk" -> Locale.TRADITIONAL_CHINESE
        else -> Locale.forLanguageTag(code)
    }
    private fun languageCode(locale: Locale?): String? = when {
        locale == null || locale == Locale.ROOT -> null
        locale.language == "pt" && locale.country == "BR" -> "pt_br"
        locale.language == "es" && locale.country == "419" -> "es_419"
        locale.language == "zh" && locale.country == "TW" -> "zh_hk"
        else -> locale.language
    }

    private companion object {
        val CHAPTER_COUNT = Regex("([0-9,]+) chapters")
        val LANGUAGES = mapOf(
            "en" to setOf("🇬🇧", "🇺🇸"), "fr" to setOf("🇫🇷"), "de" to setOf("🇩🇪"),
            "it" to setOf("🇮🇹"), "ja" to setOf("🇯🇵"), "ko" to setOf("🇰🇷"),
            "es" to setOf("🇪🇸"), "es_419" to setOf("🇲🇽"), "pt" to setOf("🇵🇹"),
            "pt_br" to setOf("🇧🇷"), "zh" to setOf("🇨🇳"), "zh_hk" to setOf("🇹🇼", "🇭🇰"),
            "ru" to setOf("🇷🇺"), "id" to setOf("🇮🇩"), "ar" to setOf("🇸🇦", "🇦🇪"),
            "th" to setOf("🇹🇭"), "vi" to setOf("🇻🇳"), "tr" to setOf("🇹🇷"),
            "pl" to setOf("🇵🇱"), "uk" to setOf("🇺🇦"), "fil" to setOf("🇵🇭"),
        )
        val GENRES = listOf(
            "Action" to "action", "Adult" to "adult", "Adventure" to "adventure", "Comedy" to "comedy",
            "Cooking" to "cooking", "Crime" to "crime", "Drama" to "drama", "Fantasy" to "fantasy",
            "Harem" to "harem", "Historical" to "historical", "Isekai" to "isekai", "Magic" to "magic",
            "Mature" to "mature", "Mystery" to "mystery", "Romance" to "romance", "School Life" to "school_life",
            "Sci-fi" to "sci_fi", "Shounen" to "shounen", "Shounen Ai" to "shounen_ai",
            "Slice of Life" to "slice_of_life", "Supernatural" to "supernatural", "Thriller" to "thriller",
            "Uncensored" to "uncensored",
        )
        val BROWSE = """query(${'$'}select:Title_Browse_Select){get_title_browse_items(select:${'$'}select){id data{
            title native_title cover_local_url cover_url content_rating_id}}}"""
        val TITLE = """query(${'$'}id:ID!){get_title_titleNode(id:${'$'}id){id data{
            title alt_titles authors artists description genre_ids vote_avg content_rating_id}}}"""
        val COMIC = """query(${'$'}id:ID!){get_comicNode(id:${'$'}id){id data{
            isPublic dbStatus translatedLanguage contentRating originalStatus summary{text}}}}"""
        val CHAPTERS = """query(${'$'}select:Select_Comic_ChapterList){get_comic_chapterList_fullList(select:${'$'}select){
            paging{next total} items{id data{id dname chaNum serial volNum datePublic srcName}}}}"""
        val PAGES = """query(${'$'}id:ID!){get_chapterNode(id:${'$'}id){id data{imageUrls}}}"""
    }
}

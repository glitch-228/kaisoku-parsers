package org.koitharu.kotatsu.parsers.site.ar

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import kotlinx.coroutines.CancellationException
import org.koitharu.kotatsu.parsers.util.json.*
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.toJSONObjectOrNull
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("AZORAMOON", "Azorafly", "ar")
internal class Azoramoon(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.AZORAMOON, 24) {

    override val availableSortOrders: Set<SortOrder> =
        EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY, SortOrder.ALPHABETICAL)

    override val configKeyDomain = ConfigKey.Domain("azorafly.com")

    override val filterCapabilities: MangaListFilterCapabilities
        get() = MangaListFilterCapabilities(
            isSearchSupported = true,
            isSearchWithFiltersSupported = true,
        )

    override suspend fun getFilterOptions() = MangaListFilterOptions(
        availableTags = fetchAvailableTags(),
    )

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(userAgentKey)
    }

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = buildString {
            append("https://api.")
            append(domain)
            append("/api/query")

            val params = mutableListOf<String>()

            // Add pagination
            params.add("page=$page")
            params.add("perPage=24")

            // Add search query
            if (!filter.query.isNullOrEmpty()) {
                params.add("searchTerm=${filter.query!!.urlEncoded()}")
            }

            // Add genre filters (comma-separated)
            if (filter.tags.isNotEmpty()) {
                val genreIds = filter.tags.joinToString(",") { it.key }
                params.add("genreIds=$genreIds")
            }

            // Add sort filter
            val (orderBy, orderDirection) = when (order) {
                SortOrder.UPDATED -> "lastChapterAddedAt" to "desc"
                SortOrder.POPULARITY -> "totalViews" to "desc"
                SortOrder.NEWEST -> "createdAt" to "desc"
                SortOrder.ALPHABETICAL -> "postTitle" to "asc"
                else -> "lastChapterAddedAt" to "desc"
            }
            params.add("orderBy=$orderBy")
            params.add("orderDirection=$orderDirection")

            // Append parameters
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }

        val response = webClient.httpGet(url)
        val body = response.body.string()

        // Try to parse as JSONArray first (API returns direct array)
        val jsonArray = try {
            JSONArray(body)
        } catch (e: Exception) {
            // If that fails, try as JSONObject and extract array
            try {
                val jsonObject = JSONObject(body)
                when {
                    jsonObject.has("posts") -> jsonObject.getJSONArray("posts")
                    jsonObject.has("data") -> jsonObject.getJSONArray("data")
                    jsonObject.has("results") -> jsonObject.getJSONArray("results")
                    else -> JSONArray()
                }
            } catch (e2: Exception) {
                JSONArray()
            }
        }

        return parseMangaList(jsonArray)
    }

    private fun parseMangaList(json: JSONArray): List<Manga> {
        val result = mutableListOf<Manga>()

        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            val slug = obj.getString("slug")
            val url = "/series/$slug"
            val title = obj.getString("postTitle")
            val coverUrl = obj.optString("featuredImage")

            // Parse status
            val seriesStatus = obj.optString("seriesStatus", "")
            val state = when (seriesStatus.uppercase()) {
                "ONGOING" -> MangaState.ONGOING
                "COMPLETED" -> MangaState.FINISHED
                "HIATUS" -> MangaState.PAUSED
                else -> null
            }

            // Parse genres
            val genresArray = obj.optJSONArray("genres")
            var isNovel = false
            val tags = if (genresArray != null) {
                buildSet {
                    for (idx in 0 until genresArray.length()) {
                        val genre = genresArray.getJSONObject(idx)
                        val genreName = genre.getString("name")
                        if (genreName == "رواية") isNovel = true
                        add(MangaTag(
                            key = genre.getInt("id").toString(),
                            title = genreName,
                            source = source,
                        ))
                    }
                }
            } else {
                emptySet()
            }

            if (obj.optString("postType") == "رواية" || obj.optString("type") == "رواية") isNovel = true

            if (obj.optString("seriesType").equals("NOVEL", ignoreCase = true)) isNovel = true

            if (slug.contains("novel", ignoreCase = true)) isNovel = true

            if (isNovel) continue

            result.add(
                Manga(
                    id = generateUid(url),
                    title = title,
                    altTitles = emptySet(),
                    url = url,
                    publicUrl = url.toAbsoluteUrl(domain),
                    rating = RATING_UNKNOWN,
                    contentRating = null,
                    coverUrl = coverUrl,
                    tags = tags,
                    state = state,
                    authors = emptySet(),
                    source = source,
                )
            )
        }

        return result
    }

    private suspend fun fetchAvailableTags(): Set<MangaTag> {
        // Hardcoded genre list from the API (swapping key and title as requested)
        return setOf(
            MangaTag("أكشن", "1", source),
            MangaTag("حريم", "2", source),
            MangaTag("زمكاني", "3", source),
            MangaTag("سحر", "4", source),
            MangaTag("شونين", "5", source),
            MangaTag("مغامرات", "6", source),
            MangaTag("خيال", "7", source),
            MangaTag("رومانسي", "8", source),
            MangaTag("كوميدي", "9", source),
            MangaTag("مانهوا", "10", source),
            MangaTag("إثارة", "11", source),
            MangaTag("دراما", "12", source),
            MangaTag("تاريخي", "13", source),
            MangaTag("راشد", "14", source),
            MangaTag("سينين", "15", source),
            MangaTag("خارق للطبيعة", "16", source),
            MangaTag("شياطين", "17", source),
            MangaTag("حياة مدرسية", "18", source),
            MangaTag("جوسي", "19", source),
            MangaTag("مانها", "20", source),
            MangaTag("ويبتون", "21", source),
            MangaTag("شينين", "22", source),
            MangaTag("قوة خارقة", "23", source),
            MangaTag("خيال علمي", "24", source),
            MangaTag("غموض", "25", source),
            MangaTag("مأساة", "26", source),
            MangaTag("شريحة من الحياة", "27", source),
            MangaTag("فنون قتالية", "28", source),
            MangaTag("شوجو", "29", source),
            MangaTag("ايسكاي", "30", source),
            MangaTag("مصاصي الدماء", "31", source),
            MangaTag("اسبوعي", "32", source),
            MangaTag("لعبة", "33", source),
            MangaTag("نفسي", "34", source),
            MangaTag("وحوش", "35", source),
            MangaTag("الحياة اليومية", "36", source),
            MangaTag("الحياة المدرسية", "37", source),
            MangaTag("رعب", "38", source),
            MangaTag("عسكري", "39", source),
            MangaTag("رياضي", "40", source),
            MangaTag("اتشي", "41", source),
            MangaTag("ايشي", "42", source),
            MangaTag("دموي", "43", source),
            MangaTag("زومبي", "44", source),
            MangaTag("مميز", "45", source),
            MangaTag("ايسيكاي", "46", source),
            MangaTag("فنتازيا", "47", source),
            MangaTag("اشباح", "48", source),
            MangaTag("إعادة إحياء", "49", source),
            MangaTag("بطل غير اعتيادي", "50", source),
            MangaTag("ثأر", "51", source),
            MangaTag("اثارة", "52", source),
            MangaTag("تراجيدي", "53", source),
            MangaTag("طبخ", "54", source),
            MangaTag("تناسخ", "55", source),
            MangaTag("عودة بالزمن", "56", source),
            MangaTag("انتقام", "57", source),
            MangaTag("تجسيد", "58", source),
            MangaTag("فانتازيا", "59", source),
            MangaTag("عائلي", "60", source),
            MangaTag("تجسد", "61", source),
            MangaTag("العاب", "62", source),
            MangaTag("عالم اخر", "63", source),
            MangaTag("السفر عبر الزمن", "64", source),
            MangaTag("خيالي", "65", source),
            MangaTag("زمنكاني", "66", source),
            MangaTag("مغامرة", "67", source),
            MangaTag("طبي", "68", source),
            MangaTag("عصور وسطى", "69", source),
            MangaTag("ساموراي", "70", source),
            MangaTag("مافيا", "71", source),
            MangaTag("نظام", "72", source),
            MangaTag("هوس", "73", source),
            MangaTag("عصري", "74", source),
            MangaTag("بطل مجنون", "75", source),
            MangaTag("رعاية اطفال", "76", source),
            MangaTag("زواج مدبر", "77", source),
            MangaTag("تشويق", "78", source),
            MangaTag("مكتبي", "79", source),
            MangaTag("قوى خارقه", "80", source),
            MangaTag("تحقيق", "81", source),
            MangaTag("أيتام", "82", source),
            MangaTag("جوسين", "83", source),
            MangaTag("موسيقي", "84", source),
            MangaTag("قصة حقيقة", "85", source),
            MangaTag("موريم", "86", source),
            MangaTag("موظفين", "87", source),
            MangaTag("فيكتوري", "88", source),
            MangaTag("مأساوي", "89", source),
            MangaTag("عصر حديث", "90", source),
            MangaTag("ندم", "91", source),
            MangaTag("حياة جامعية", "92", source),
            MangaTag("حاصد", "93", source),
            MangaTag("الأرواح", "94", source),
            MangaTag("جريمة", "95", source),
            MangaTag("عاطفي", "96", source),
            MangaTag("أكاديمي", "97", source),
        )
    }

    override suspend fun getDetails(manga: Manga): Manga {
        try {
            return getApiDetails(manga)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Preserve saved Astro/rendered-reader support while old and new frontends coexist.
        }
        return getHtmlDetails(manga)
    }

    private suspend fun getHtmlDetails(manga: Manga): Manga {
        val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()

        if (doc.selectFirst("span.inline-block.border.text-foreground")?.text() == "رواية") {
            return manga.copy(
                state = MangaState.FINISHED,
                chapters = emptyList(),
            )
        }

        val chapters = loadChapters(manga, doc)

        val coverUrl = doc.selectFirst("section img")?.src() ?: manga.coverUrl

        // Extract rating
        val ratingText = doc.selectFirst("div:contains(التقييم) + div, span:contains(Rating)")?.text()
        val rating = ratingText?.substringBefore("/")?.trim()?.toFloatOrNull()?.div(5f) ?: RATING_UNKNOWN

        // Extract status
        val statusText = doc.selectFirst("div:contains(الحالة) + div, span:contains(Status)")?.text()
        val state = when {
            statusText?.contains("مستمر", ignoreCase = true) == true ||
            statusText?.contains("ongoing", ignoreCase = true) == true -> MangaState.ONGOING
            statusText?.contains("مكتمل", ignoreCase = true) == true ||
            statusText?.contains("completed", ignoreCase = true) == true -> MangaState.FINISHED
            else -> null
        }

        // Extract description from meta tag
        val description = doc.selectFirst("meta[name=description]")?.attr("content")

        // Extract tags/genres
        val tags = doc.select("a[href*='/series/?genres='], span.genre").mapNotNullToSet { element ->
            val genreName = element.text().trim()
            val genreId = element.attr("href").substringAfter("genres=").substringBefore("&")
                .ifEmpty { genreName }

            if (genreName.isNotEmpty()) {
                MangaTag(
                    key = genreId,
                    title = genreName,
                    source = source,
                )
            } else {
                null
            }
        }

        return manga.copy(
            coverUrl = coverUrl,
            rating = rating,
            state = state,
            tags = tags,
            description = description,
            chapters = chapters,
        )
    }

    private fun loadChapters(manga: Manga, doc: Document): List<MangaChapter> {
        val seriesSlug = manga.url.substringAfter("/series/", "").substringBefore('/')
        if (seriesSlug.isEmpty()) return emptyList()

        val chaptersJson = extractChaptersFromAstroProps(doc)
            ?: return loadRenderedChapters(seriesSlug, doc)

        val chapters = ArrayList<MangaChapter>(chaptersJson.length())
        for (i in 0 until chaptersJson.length()) {
            val chapter = unboxSerializedValue(chaptersJson.opt(i)) as? JSONObject ?: continue
            val slug = (unboxSerializedValue(chapter.opt("slug")) as? String)
                ?.takeUnless { it.isBlank() || it == "null" }
                ?: continue
            val url = "/series/$seriesSlug/$slug"
            val number = when (val rawNumber = unboxSerializedValue(chapter.opt("number"))) {
                is Number -> rawNumber.toFloat()
                is String -> rawNumber.toFloatOrNull() ?: 0f
                else -> 0f
            }
            val title = (unboxSerializedValue(chapter.opt("title")) as? String)
                ?.takeUnless { it.isBlank() || it == "null" }
            val createdAt = unboxSerializedValue(chapter.opt("createdAt")) as? String

            chapters += MangaChapter(
                id = generateUid(url),
                title = when {
                    !title.isNullOrBlank() && number > 0f -> "الفصل ${formatChapterNumber(number)} - $title"
                    !title.isNullOrBlank() -> title
                    number > 0f -> "الفصل ${formatChapterNumber(number)}"
                    else -> slug
                },
                number = number,
                volume = 0,
                url = url,
                scanlator = null,
                uploadDate = parseChapterDate(createdAt),
                branch = null,
                source = source,
            )
        }
        return chapters.sortedBy { it.number }
    }

    private fun loadRenderedChapters(seriesSlug: String, doc: Document): List<MangaChapter> {
        val prefix = "/series/$seriesSlug/"
        val chapterSlug = Regex("chapter-([0-9]+(?:[.][0-9]+)?)")
        return doc.select("a[href]").mapNotNull { link ->
            val url = link.attr("href").toRelativeUrl(domain).substringBefore('?').substringBefore('#')
            if (!url.startsWith(prefix)) return@mapNotNull null
            val match = chapterSlug.matchEntire(url.removePrefix(prefix).trimEnd('/'))
                ?: return@mapNotNull null
            val number = match.groupValues[1].toFloatOrNull()?.takeIf { it.isFinite() }
                ?: return@mapNotNull null
            MangaChapter(
                id = generateUid(url),
                title = "الفصل ${formatChapterNumber(number)}",
                number = number,
                volume = 0,
                url = url,
                scanlator = null,
                uploadDate = 0,
                branch = null,
                source = source,
            )
        }.distinctBy { it.url }.sortedBy { it.number }
    }

    private fun extractChaptersFromAstroProps(doc: Document): JSONArray? {
        val propsEscaped = doc
            .selectFirst("section[data-series-tab-panel=chapters] astro-island[props]")
            ?.attr("props")
            ?.takeUnless { it.isBlank() }
            ?: return null

        val props = Parser.unescapeEntities(propsEscaped, false)
        val root = props.toJSONObjectOrNull() ?: return null
        // The chapters island embeds the full chapter list in the "initialChap" prop
        // (the "post" prop only carries a _count.chapters number, not the array).
        (unboxSerializedValue(root.opt("initialChap")) as? JSONArray)?.let { return it }
        // Fallback for older markup that nested chapters inside "post".
        val post = unboxSerializedValue(root.opt("post")) as? JSONObject ?: return null
        return unboxSerializedValue(post.opt("chapters")) as? JSONArray
    }

    private fun unboxSerializedValue(value: Any?): Any? {
        if (value !is JSONArray || value.length() != 2) {
            return value
        }
        return when (value.opt(0)) {
            0, 1, "0", "1" -> unboxSerializedValue(value.opt(1))
            else -> value
        }
    }

    private fun formatChapterNumber(number: Float): String {
        return if (number % 1f == 0f) {
            number.toInt().toString()
        } else {
            number.toString()
        }
    }

    private fun parseChapterDate(date: String?): Long {
        if (date.isNullOrBlank()) return 0L
        return synchronized(chapterDateFormat) { chapterDateFormat.parseSafe(date) }
    }

    private val chapterDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        try {
            return getApiPages(chapter)
        } catch (error: CancellationException) {
            throw error
        } catch (error: LockedChapterException) {
            throw ParseException("Chapter is locked", error.url)
        } catch (_: Exception) {
            // The legacy frontend may still carry a directly readable image list.
        }
        val pages = getHtmlPages(chapter)
        if (pages.isEmpty()) throw ParseException("Cannot find chapter images", chapter.url)
        return pages
    }

    private class LockedChapterException(val url: String) : RuntimeException("Chapter is locked")

    private suspend fun getHtmlPages(chapter: MangaChapter): List<MangaPage> {
        val fullUrl = chapter.url.toAbsoluteUrl(domain)
        val doc = webClient.httpGet(fullUrl).parseHtml()

        return doc.select("section[itemprop=articleBody] figure img").mapNotNull { img ->
            val imageUrl = img.src()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            MangaPage(
                id = generateUid(imageUrl),
                url = imageUrl,
                preview = null,
                source = source,
            )
        }
    }
    private suspend fun getApiDetails(manga: Manga): Manga {
        val seriesSlug = manga.url.substringAfter("/series/").substringBefore('/')
        val json = webClient.httpGet("https://api.$domain/api/post?postSlug=${seriesSlug.urlEncoded()}").parseJson()
        val post = json.getJSONObject("post")

        val isNovel = post.getBooleanOrDefault("isNovel", false) ||
            post.getStringOrNull("seriesType").equals("NOVEL", ignoreCase = true)

        val tags = post.optJSONArray("genres")?.mapJSONNotNullToSet { genre ->
            val name = genre.getStringOrNull("name") ?: return@mapJSONNotNullToSet null
            MangaTag(
                key = genre.getStringOrNull("id") ?: return@mapJSONNotNullToSet null,
                title = name,
                source = source,
            )
        }.orEmpty()

        val rating = post.getFloatOrDefault("averageRating", 0f)
        val chapters = if (isNovel) emptyList() else loadApiChapters(seriesSlug, post, json.getIntOrDefault("totalChapterCount", 0))

        return manga.copy(
            title = post.getStringOrNull("postTitle") ?: manga.title,
            altTitles = setOfNotNull(post.getStringOrNull("alternativeTitles")),
            coverUrl = post.getStringOrNull("featuredImage") ?: manga.coverUrl,
            rating = if (rating > 0f) (rating / 10f).coerceAtMost(1f) else RATING_UNKNOWN,
            state = parseState(post.getStringOrNull("seriesStatus")) ?: manga.state,
            authors = setOfNotNull(post.getStringOrNull("author"), post.getStringOrNull("artist")),
            tags = tags.ifEmpty { manga.tags },
            description = post.getStringOrNull("postContent"),
            chapters = chapters,
        )
    }

    private suspend fun loadApiChapters(seriesSlug: String, post: JSONObject, totalCount: Int): List<MangaChapter> {
        // The post endpoint may embed a partial (or empty) chapter list; the chapters endpoint returns all of them.
        val embedded = post.optJSONArray("chapters")
        val chaptersJson = if (embedded != null && embedded.length() > 0 && embedded.length() >= totalCount) {
            embedded
        } else {
            webClient.httpGet("https://api.$domain/api/chapters?postId=${post.getLong("id")}")
                .parseJson()
                .getJSONObject("post")
                .getJSONArray("chapters")
        }

        require(chaptersJson.length() <= 20_000) { "Azorafly chapter list exceeds its safe limit" }
        // The API lists chapters newest first
        return chaptersJson.mapChapters(reversed = true) { _, chapter ->
            if (chapter.isLocked()) return@mapChapters null
            val slug = chapter.getStringOrNull("slug") ?: return@mapChapters null
            val url = "/series/$seriesSlug/$slug"
            val number = chapter.getDouble("number").toFloat()
            require(number.isFinite() && number >= 0) { "Invalid Azorafly chapter number" }
            val title = chapter.getStringOrNull("title")
            MangaChapter(
                id = generateUid(url),
                title = when {
                    title != null && number > 0f -> "الفصل ${formatChapterNumber(number)} - $title"
                    title != null -> title
                    number > 0f -> "الفصل ${formatChapterNumber(number)}"
                    else -> slug
                },
                number = number,
                volume = 0,
                url = url,
                scanlator = null,
                uploadDate = parseChapterDate(chapter.getStringOrNull("createdAt")),
                branch = null,
                source = source,
            )
        }.sortedBy { it.number }
    }

    // Paid chapters, same rule as the site's Iken theme: they have no images until bought
    private fun JSONObject.isLocked(): Boolean =
        getBooleanOrDefault("isLocked", false) ||
            getBooleanOrDefault("isTimeLocked", false) ||
            (!getBooleanOrDefault("chapterPurchased", false) && getIntOrDefault("price", 0) != 0)

    private fun parseState(status: String?): MangaState? = when (status?.uppercase()) {
        "ONGOING", "MASS_RELEASED" -> MangaState.ONGOING
        "COMPLETED" -> MangaState.FINISHED
        "HIATUS" -> MangaState.PAUSED
        "CANCELLED", "DROPPED" -> MangaState.ABANDONED
        "COMING_SOON" -> MangaState.UPCOMING
        else -> null
    }


    private suspend fun getApiPages(chapter: MangaChapter): List<MangaPage> {
        val seriesSlug = chapter.url.substringAfter("/series/").substringBefore('/')
        val chapterSlug = chapter.url.substringAfterLast('/')
        val url = "https://api.$domain/api/chapter?mangaslug=${seriesSlug.urlEncoded()}&chapterslug=${chapterSlug.urlEncoded()}"
        val data = webClient.httpGet(url).parseJson().getJSONObject("chapter")

        if (data.getBooleanOrDefault("isPermanentlyLocked", false) ||
            data.getBooleanOrDefault("isLockedByCoins", false) ||
            data.getBooleanOrDefault("isShortLinkLocked", false)
        ) {
            throw LockedChapterException(chapter.url.toAbsoluteUrl(domain))
        }

        val images = data.getJSONArray("images")
        require(images.length() in 1..5000) { "Azorafly returned an empty or oversized reader" }
        return images.asTypedList<JSONObject>()
            .sortedBy { it.getIntOrDefault("order", Int.MAX_VALUE) }
            .mapNotNull { image ->
                val imageUrl = image.getString("url").replace(" ", "%20").toAbsoluteUrl(domain)
                MangaPage(
                    id = generateUid(imageUrl),
                    url = imageUrl,
                    preview = null,
                    source = source,
                )
            }
    }
}

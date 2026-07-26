package org.koitharu.kotatsu.parsers.site.all

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import org.jsoup.Jsoup
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.bitmap.Rect
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.ContentRating
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
import org.koitharu.kotatsu.parsers.network.OkHttpWebClient
import org.koitharu.kotatsu.parsers.network.WebClient
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.getCookies
import org.koitharu.kotatsu.parsers.util.parseFailed
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import org.koitharu.kotatsu.parsers.util.toTitleCase
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.EnumSet
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.min
import okhttp3.Protocol
import okhttp3.Response

private const val PIECE_SIZE = 200
private const val MIN_SPLIT_COUNT = 5

@Suppress("CustomX509TrustManager")
internal abstract class MangaFireParser(
    context: MangaLoaderContext,
    source: MangaParserSource,
    private val siteLang: String,
) : PagedMangaParser(context, source, 30), Interceptor, MangaParserAuthProvider {

    private val imageHttp11Client by lazy {
        context.httpClient.newBuilder()
            .apply {
                interceptors().clear()
                networkInterceptors().clear()
            }
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()
    }

    private val client: WebClient by lazy {
        val newHttpClient = context.httpClient.newBuilder()
            .sslSocketFactory(SSLUtils.sslSocketFactory!!, SSLUtils.trustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(
                    request.newBuilder()
                        .addHeader("Referer", "https://$domain/")
                        .build(),
                )

                if (request.url.fragment?.startsWith("scrambled") == true) {
                    return@addInterceptor context.redrawImageResponse(response) { bitmap ->
                        val offset = request.url.fragment!!.substringAfter("_").toInt()
                        val width = bitmap.width
                        val height = bitmap.height

                        val result = context.createBitmap(width, height)

                        val pieceWidth = min(PIECE_SIZE, width.ceilDiv(MIN_SPLIT_COUNT))
                        val pieceHeight = min(PIECE_SIZE, height.ceilDiv(MIN_SPLIT_COUNT))
                        val xMax = width.ceilDiv(pieceWidth) - 1
                        val yMax = height.ceilDiv(pieceHeight) - 1

                        for (y in 0..yMax) {
                            for (x in 0..xMax) {
                                val xDst = pieceWidth * x
                                val yDst = pieceHeight * y
                                val w = min(pieceWidth, width - xDst)
                                val h = min(pieceHeight, height - yDst)

                                val xSrc = pieceWidth * when (x) {
                                    xMax -> x // margin
                                    else -> (xMax - x + offset) % xMax
                                }
                                val ySrc = pieceHeight * when (y) {
                                    yMax -> y // margin
                                    else -> (yMax - y + offset) % yMax
                                }

                                val srcRect = Rect(xSrc, ySrc, xSrc + w, ySrc + h)
                                val dstRect = Rect(xDst, yDst, xDst + w, yDst + h)

                                result.drawBitmap(bitmap, srcRect, dstRect)
                            }
                        }

                        result
                    }
                }

                response
            }
            .build()
        OkHttpWebClient(newHttpClient, source)
    }

    override val configKeyDomain: ConfigKey.Domain = ConfigKey.Domain("mangafire.to")

    override val availableSortOrders: Set<SortOrder> = EnumSet.of(
        SortOrder.UPDATED,
        SortOrder.POPULARITY,
        SortOrder.RATING,
        SortOrder.NEWEST,
        SortOrder.ALPHABETICAL,
        SortOrder.RELEVANCE,
    )

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(userAgentKey)
    }

    override val authUrl: String
        get() = "https://${domain}"

    override suspend fun isAuthorized(): Boolean {
        return context.cookieJar.getCookies(domain).any {
            it.value.contains("user")
        }
    }

    override suspend fun getUsername(): String {
        val body = client.httpGet("https://${domain}/user/profile").parseHtml().body()
        return body.selectFirst("form.ajax input[name*=username]")?.attr("value")
            ?: body.parseFailed("Cannot find username")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .removeHeader("Referer")
            .addHeader("Referer", "https://$domain/")
            .build()

        val response = if (request.url.host.contains("mfcdn")) {
            imageHttp11Client.newCall(newRequest).execute()
        } else {
            chain.proceed(newRequest)
        }

        if (request.url.fragment?.startsWith("scrambled") == true) {
            return context.redrawImageResponse(response) { bitmap ->
                val offset = request.url.fragment!!.substringAfter("_").toInt()
                val width = bitmap.width
                val height = bitmap.height

                val result = context.createBitmap(width, height)

                val pieceWidth = min(PIECE_SIZE, width.ceilDiv(MIN_SPLIT_COUNT))
                val pieceHeight = min(PIECE_SIZE, height.ceilDiv(MIN_SPLIT_COUNT))
                val xMax = width.ceilDiv(pieceWidth) - 1
                val yMax = height.ceilDiv(pieceHeight) - 1

                for (y in 0..yMax) {
                    for (x in 0..xMax) {
                        val xDst = pieceWidth * x
                        val yDst = pieceHeight * y
                        val w = min(pieceWidth, width - xDst)
                        val h = min(pieceHeight, height - yDst)

                        val xSrc = pieceWidth * when (x) {
                            xMax -> x
                            else -> (xMax - x + offset) % xMax
                        }
                        val ySrc = pieceHeight * when (y) {
                            yMax -> y
                            else -> (yMax - y + offset) % yMax
                        }

                        val srcRect = Rect(xSrc, ySrc, xSrc + w, ySrc + h)
                        val dstRect = Rect(xDst, yDst, xDst + w, yDst + h)

                        result.drawBitmap(bitmap, srcRect, dstRect)
                    }
                }

                result
            }
        }

        return response
    }

    private val tags = GENRE_MAP.map { (title, id) ->
        MangaTag(title, id, source)
    }.toSet()

    private val tagsByTitle = tags.associateBy { it.title }

    companion object {
        private val GENRE_MAP = mapOf(
            "Action" to "1",
            "Adventure" to "78",
            "Avant Garde" to "3",
            "Boys Love" to "4",
            "Comedy" to "5",
            "Demons" to "77",
            "Drama" to "6",
            "Ecchi" to "7",
            "Fantasy" to "79",
            "Girls Love" to "9",
            "Gourmet" to "10",
            "Harem" to "11",
            "Horror" to "530",
            "Isekai" to "13",
            "Iyashikei" to "531",
            "Josei" to "15",
            "Kids" to "532",
            "Magic" to "539",
            "Mahou Shoujo" to "533",
            "Martial Arts" to "534",
            "Mecha" to "19",
            "Military" to "535",
            "Music" to "21",
            "Mystery" to "22",
            "Parody" to "23",
            "Psychological" to "536",
            "Reverse Harem" to "25",
            "Romance" to "26",
            "School" to "73",
            "Sci-Fi" to "28",
            "Seinen" to "537",
            "Shoujo" to "30",
            "Shounen" to "31",
            "Slice of Life" to "538",
            "Space" to "33",
            "Sports" to "34",
            "Super Power" to "75",
            "Supernatural" to "76",
            "Suspense" to "37",
            "Thriller" to "38",
            "Vampire" to "39",
        )
    }

    override val filterCapabilities: MangaListFilterCapabilities
        get() = MangaListFilterCapabilities(
            isMultipleTagsSupported = true,
            isTagsExclusionSupported = true,
            isSearchSupported = true,
        )

    override suspend fun getFilterOptions() = MangaListFilterOptions(
        availableTags = tags,
        availableStates = EnumSet.of(
            MangaState.ONGOING,
            MangaState.FINISHED,
            MangaState.ABANDONED,
            MangaState.PAUSED,
            MangaState.UPCOMING,
        ),
    )

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = "https://$domain/api/titles".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", pageSize.toString())
            .addQueryParameter("language[]", siteLang)
            .apply {
                filter.query?.takeUnless { it.isBlank() }?.let {
                    addQueryParameter("keyword", it)
                }
                filter.tags.forEach {
                    addQueryParameter("genres_in[]", it.key)
                }
                filter.tagsExclude.forEach {
                    addQueryParameter("genres_ex[]", it.key)
                }
                filter.states.forEach {
                    addQueryParameter("statuses[]", it.toMangaFireStatus())
                }
                order.toMangaFireSort()?.let { (key, value) ->
                    addQueryParameter("order[$key]", value)
                }
            }
            .build()

        return client.httpGet(url).parseJson().getJSONArray("items").mapObjects { data ->
            data.toManga()
        }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val hid = extractHid(manga.url)
        val data = client.httpGet("https://$domain/api/titles/$hid")
            .parseJson()
            .getJSONObject("data")
        val genreTags = data.optJSONArray("genres").mapObjects { tag ->
            tagsByTitle[tag.optString("title")]
        }.filterNotNull().toSet()
        val themeTags = data.optJSONArray("themes").mapObjects { tag ->
            tagsByTitle[tag.optString("title")]
        }.filterNotNull()
        val allTags = genreTags + themeTags
        val authors = data.optJSONArray("authors").mapTitles() + data.optJSONArray("artists").mapTitles()
        val ratingValue = data.optDouble("rating", RATING_UNKNOWN.toDouble()).toFloat().let {
            if (it <= 0f) RATING_UNKNOWN else it / 2f
        }

        return manga.copy(
            title = data.optString("title", manga.title),
            altTitles = data.optJSONArray("altTitles").mapStrings().toSet(),
            rating = ratingValue,
            coverUrl = data.optJSONObject("poster")?.optString("large").orEmpty()
                .ifBlank { manga.coverUrl },
            tags = allTags,
            contentRating = allTags.toContentRating(),
            state = data.optString("status").toMangaState(),
            authors = authors,
            description = data.optString("synopsisHtml")
                .takeUnless { it.isBlank() }
                ?.let { Jsoup.parseBodyFragment(it).text() },
            chapters = getChapters(hid, manga.url, data.optBoolean("hasVolumes", false)),
        )
    }

    private suspend fun getChapters(
        hid: String,
        mangaUrl: String,
        hasVolumes: Boolean,
    ): List<MangaChapter> {
        val url = "https://$domain/api/titles/$hid/chapters".toHttpUrl().newBuilder()
            .addQueryParameter("page", "1")
            .addQueryParameter("limit", "500")
            .build()
        val chapters = client.httpGet(url).parseJson().getJSONArray("items").mapObjects { data ->
            if (data.optString("language") != siteLang) {
                return@mapObjects null
            }
            val id = data.getLong("id")
            val number = data.optDouble("number", -1.0).toFloat()
            val name = data.optString("name")
            val type = data.optString("type")
            MangaChapter(
                id = generateUid("$mangaUrl/$id"),
                title = buildString {
                    append("Chapter ")
                    append(number.formatChapterNumber())
                    if (name.isNotBlank()) {
                        append(": ")
                        append(name)
                    }
                },
                number = number,
                volume = 0,
                url = "$mangaUrl/$id",
                scanlator = null,
                uploadDate = data.optLong("createdAt", 0L) * 1000L,
                branch = type,
                source = source,
            )
        }.filterNotNull().toMutableList()
        if (hasVolumes) {
            val volumesUrl = "https://$domain/api/titles/$hid/volumes".toHttpUrl().newBuilder()
                .addQueryParameter("language", siteLang)
                .build()
            client.httpGet(volumesUrl).parseJson().optJSONArray("items").mapObjects { data ->
                if (data.optString("language") != siteLang) {
                    return@mapObjects null
                }
                val id = data.getLong("id")
                val number = data.optDouble("number", -1.0).toFloat()
                val name = data.optString("name")
                MangaChapter(
                    id = generateUid("$mangaUrl/vol/$id"),
                    title = buildString {
                        append("Volume ")
                        append(number.formatChapterNumber())
                        if (name.isNotBlank()) {
                            append(": ")
                            append(name)
                        }
                    },
                    number = number,
                    volume = 0,
                    url = "$mangaUrl/vol/$id",
                    scanlator = data.optInt("chapterCount", 0).takeIf { it > 0 }
                        ?.let { "$it chapters" },
                    uploadDate = 0L,
                    branch = "volume",
                    source = source,
                )
            }.filterNotNullTo(chapters)
        }
        val types = chapters.mapNotNull { it.branch }.distinct()
        return chapters
            .map { chapter ->
                chapter.copy(
                    branch = if (types.size > 1) {
                        chapter.branch?.toTitleCase(sourceLocale)
                    } else {
                        null
                    },
                )
            }
            .sortedBy { it.number }
    }

    override suspend fun getRelatedManga(seed: Manga): List<Manga> {
        return emptyList()
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val chapterId = chapter.url.substringAfterLast("/")
        val endpoint = if ("/vol/" in chapter.url) "volumes" else "chapters"
        return client.httpGet("https://$domain/api/$endpoint/$chapterId")
            .parseJson()
            .getJSONObject("data")
            .getJSONArray("pages")
            .mapObjects { data ->
                val url = data.getString("url")
                MangaPage(
                    id = generateUid(url),
                    url = url,
                    preview = null,
                    source = source,
                )
            }
    }

    private fun JSONObject.toManga(): Manga {
        val url = optString("url").ifBlank {
            "/title/${getString("hid")}-${getString("slug")}"
        }
        val poster = optJSONObject("poster")
        return Manga(
            id = generateUid(url),
            url = url,
            publicUrl = url.toAbsoluteUrl(domain),
            title = getString("title"),
            coverUrl = poster?.optString("large").orEmpty()
                .ifBlank { poster?.optString("medium").orEmpty() },
            source = source,
            altTitles = emptySet(),
            largeCoverUrl = poster?.optString("large").orEmpty().ifBlank { null },
            authors = emptySet(),
            contentRating = null,
            rating = RATING_UNKNOWN,
            state = optString("status").toMangaState(),
            tags = emptySet(),
        )
    }

    private fun JSONArray?.mapStrings(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { i -> getString(i) }
    }

    private fun JSONArray?.mapTitles(): Set<String> {
        if (this == null) return emptySet()
        return mapObjects { it.optString("title") }.filter { it.isNotBlank() }.toSet()
    }

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return List(length()) { i -> transform(getJSONObject(i)) }
    }

    private fun extractHid(url: String): String {
        val lastPart = url.removeSuffix("/").substringAfterLast("/")
        return when {
            lastPart.contains(".") -> lastPart.substringAfterLast(".")
            lastPart.contains("-") -> lastPart.substringBefore("-")
            else -> lastPart
        }
    }

    private fun MangaState.toMangaFireStatus(): String = when (this) {
        MangaState.ONGOING -> "releasing"
        MangaState.FINISHED -> "finished"
        MangaState.ABANDONED -> "discontinued"
        MangaState.PAUSED -> "on_hiatus"
        MangaState.UPCOMING -> "not_yet_released"
        else -> ""
    }

    private fun String.toMangaState(): MangaState? = when (lowercase()) {
        "releasing" -> MangaState.ONGOING
        "finished", "completed" -> MangaState.FINISHED
        "discontinued" -> MangaState.ABANDONED
        "on_hiatus" -> MangaState.PAUSED
        "not_yet_released", "info" -> MangaState.UPCOMING
        else -> null
    }

    private fun SortOrder.toMangaFireSort(): Pair<String, String>? = when (this) {
        SortOrder.UPDATED -> "chapter_updated_at" to "desc"
        SortOrder.POPULARITY -> "views_total" to "desc"
        SortOrder.RATING -> "score" to "desc"
        SortOrder.NEWEST -> "created_at" to "desc"
        SortOrder.ALPHABETICAL -> "title" to "asc"
        SortOrder.RELEVANCE -> "relevance" to "desc"
        else -> null
    }

    private fun Set<MangaTag>.toContentRating(): ContentRating = when {
        any { it.title == "Hentai" } -> ContentRating.ADULT
        any { it.title == "Ecchi" } -> ContentRating.SUGGESTIVE
        else -> ContentRating.SAFE
    }

    private fun Float.formatChapterNumber(): String {
        return if (rem(1f) == 0f) {
            toInt().toString()
        } else {
            toString()
        }
    }

    private fun Int.ceilDiv(other: Int) = (this + (other - 1)) / other

    private fun encodeKeyword(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            when {
                c == ' ' -> sb.append('+')
                c.isLetterOrDigit() || c.code > 0x7F -> sb.append(c)
                else -> sb.append(String.format("%%%02X", c.code))
            }
        }
        return sb.toString()
    }

    /*
     * mangafire.to was rebuilt as a client-rendered SPA on the same platform Comix now runs: every
     * page (home, /filter, /title/...) returns the same ~3 KB shell carrying only `window.__config`
     * and `window.__build`, with no manga markup for these selectors to find. Content comes from
     * `GET /api/titles` and friends, which answer 403 `{"message":"Missing token."}` without the
     * short-lived `_` signature the SPA generates (`/api/top-titles` and `/api/me` are open, but not
     * enough to browse or read). Reviving this needs the WebView + JSON.parse capture architecture
     * ComixParser already uses, which is a rewrite rather than a selector fix.
     */
    @Broken("Site is now an SPA; API needs a signed token — needs a WebView-capture rewrite")
    @MangaSourceParser("MANGAFIRE_EN", "MangaFire English", "en")
    class English(context: MangaLoaderContext) : MangaFireParser(context, MangaParserSource.MANGAFIRE_EN, "en")

    @Broken("Site is now an SPA; API needs a signed token — needs a WebView-capture rewrite")
    @MangaSourceParser("MANGAFIRE_ES", "MangaFire Spanish", "es")
    class Spanish(context: MangaLoaderContext) : MangaFireParser(context, MangaParserSource.MANGAFIRE_ES, "es")

    @Broken("Site is now an SPA; API needs a signed token — needs a WebView-capture rewrite")
    @MangaSourceParser("MANGAFIRE_ESLA", "MangaFire Spanish (Latim)", "es")
    class SpanishLatim(context: MangaLoaderContext) :
        MangaFireParser(context, MangaParserSource.MANGAFIRE_ESLA, "es-la")

    @Broken("Site is now an SPA; API needs a signed token — needs a WebView-capture rewrite")
    @MangaSourceParser("MANGAFIRE_FR", "MangaFire French", "fr")
    class French(context: MangaLoaderContext) : MangaFireParser(context, MangaParserSource.MANGAFIRE_FR, "fr")

    @Broken("Site is now an SPA; API needs a signed token — needs a WebView-capture rewrite")
    @MangaSourceParser("MANGAFIRE_JA", "MangaFire Japanese", "ja")
    class Japanese(context: MangaLoaderContext) : MangaFireParser(context, MangaParserSource.MANGAFIRE_JA, "ja")

    @Broken("Site is now an SPA; API needs a signed token — needs a WebView-capture rewrite")
    @MangaSourceParser("MANGAFIRE_PT", "MangaFire Portuguese", "pt")
    class Portuguese(context: MangaLoaderContext) : MangaFireParser(context, MangaParserSource.MANGAFIRE_PT, "pt")

    @Broken("Site is now an SPA; API needs a signed token — needs a WebView-capture rewrite")
    @MangaSourceParser("MANGAFIRE_PTBR", "MangaFire Portuguese (Brazil)", "pt")
    class PortugueseBR(context: MangaLoaderContext) :
        MangaFireParser(context, MangaParserSource.MANGAFIRE_PTBR, "pt-br")
}

public object SSLUtils {
    public val trustAllCerts: Array<TrustManager> = arrayOf(@Suppress("CustomX509TrustManager")
    object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
    })

    public val sslSocketFactory: SSLSocketFactory? = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }.socketFactory

    public val trustManager: X509TrustManager = trustAllCerts[0] as X509TrustManager
}

public object VrfGenerator {
    private val rc4Keys = mapOf(
        "l" to "FgxyJUQDPUGSzwbAq/ToWn4/e8jYzvabE+dLMb1XU1o=",
        "g" to "CQx3CLwswJAnM1VxOqX+y+f3eUns03ulxv8Z+0gUyik=",
        "B" to "fAS+otFLkKsKAJzu3yU+rGOlbbFVq+u+LaS6+s1eCJs=",
        "m" to "Oy45fQVK9kq9019+VysXVlz1F9S1YwYKgXyzGlZrijo=",
        "F" to "aoDIdXezm2l3HrcnQdkPJTDT8+W6mcl2/02ewBHfPzg=",
    )

    private val seeds32 = mapOf(
        "A" to "yH6MXnMEcDVWO/9a6P9W92BAh1eRLVFxFlWTHUqQ474=",
        "V" to "RK7y4dZ0azs9Uqz+bbFB46Bx2K9EHg74ndxknY9uknA=",
        "N" to "rqr9HeTQOg8TlFiIGZpJaxcvAaKHwMwrkqojJCpcvoc=",
        "P" to "/4GPpmZXYpn5RpkP7FC/dt8SXz7W30nUZTe8wb+3xmU=",
        "k" to "wsSGSBXKWA9q1oDJpjtJddVxH+evCfL5SO9HZnUDFU8=",
    )

    private val prefixKeys = mapOf(
        "O" to "l9PavRg=",
        "v" to "Ml2v7ag1Jg==",
        "L" to "i/Va0UxrbMo=",
        "p" to "WFjKAHGEkQM=",
        "W" to "5Rr27rWd",
    )

    private fun add8(n: Int): (Int) -> Int = { c -> (c + n) and 0xFF }
    private fun sub8(n: Int): (Int) -> Int = { c -> (c - n + 256) and 0xFF }
    private fun rotl8(n: Int): (Int) -> Int = { c -> ((c shl n) or (c ushr (8 - n))) and 0xFF }
    private fun rotr8(n: Int): (Int) -> Int = { c -> ((c ushr n) or (c shl (8 - n))) and 0xFF }

    private val scheduleC = listOf(
        sub8(223), rotr8(4), rotr8(4), add8(234), rotr8(7),
        rotr8(2), rotr8(7), sub8(223), rotr8(7), rotr8(6),
    )

    private val scheduleY = listOf(
        add8(19), rotr8(7), add8(19), rotr8(6), add8(19),
        rotr8(1), add8(19), rotr8(6), rotr8(7), rotr8(4),
    )

    private val scheduleB = listOf(
        sub8(223), rotr8(1), add8(19), sub8(223), rotl8(2),
        sub8(223), add8(19), rotl8(1), rotl8(2), rotl8(1),
    )

    private val scheduleJ = listOf(
        add8(19), rotl8(1), rotl8(1), rotr8(1), add8(234),
        rotl8(1), sub8(223), rotl8(6), rotl8(4), rotl8(1),
    )

    private val scheduleE = listOf(
        rotr8(1), rotl8(1), rotl8(6), rotr8(1), rotl8(2),
        rotr8(4), rotl8(1), rotl8(1), sub8(223), rotl8(2),
    )

    public fun generate(input: String): String {
        val encodedInput = URLEncoder.encode(input, "UTF-8").replace("+", "%20")
        var bytes = encodedInput.toByteArray(Charsets.UTF_8)

        bytes = rc4(atob(rc4Keys["l"]!!), bytes)
        bytes = transform(bytes, atob(seeds32["A"]!!), atob(prefixKeys["O"]!!), scheduleC)

        bytes = rc4(atob(rc4Keys["g"]!!), bytes)
        bytes = transform(bytes, atob(seeds32["V"]!!), atob(prefixKeys["v"]!!), scheduleY)

        bytes = rc4(atob(rc4Keys["B"]!!), bytes)
        bytes = transform(bytes, atob(seeds32["N"]!!), atob(prefixKeys["L"]!!), scheduleB)

        bytes = rc4(atob(rc4Keys["m"]!!), bytes)
        bytes = transform(bytes, atob(seeds32["P"]!!), atob(prefixKeys["p"]!!), scheduleJ)

        bytes = rc4(atob(rc4Keys["F"]!!), bytes)
        bytes = transform(bytes, atob(seeds32["k"]!!), atob(prefixKeys["W"]!!), scheduleE)

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun atob(str: String): ByteArray = Base64.getDecoder().decode(str)

    private fun rc4(key: ByteArray, input: ByteArray): ByteArray {
        val s = IntArray(256) { it }
        var j = 0

        for (i in 0..255) {
            j = (j + s[i] + key[i % key.size].toInt().and(0xFF)) and 0xFF
            val temp = s[i]
            s[i] = s[j]
            s[j] = temp
        }

        val output = ByteArray(input.size)
        var i = 0
        j = 0

        for (k in input.indices) {
            i = (i + 1) and 0xFF
            j = (j + s[i]) and 0xFF

            val temp = s[i]
            s[i] = s[j]
            s[j] = temp

            val t = (s[i] + s[j]) and 0xFF
            val kByte = s[t]
            output[k] = (input[k].toInt() xor kByte).toByte()
        }

        return output
    }

    private fun transform(
        input: ByteArray,
        seed: ByteArray,
        prefix: ByteArray,
        schedule: List<(Int) -> Int>,
    ): ByteArray {
        val out = ByteArrayOutputStream()

        for (i in input.indices) {
            if (i < prefix.size) {
                out.write(prefix[i].toInt())
            }

            val inputByte = input[i].toInt() and 0xFF
            val seedByte = seed[i % 32].toInt() and 0xFF
            val xored = inputByte xor seedByte
            val transformed = schedule[i % 10](xored)
            out.write(transformed)
        }

        return out.toByteArray()
    }
}

package org.koitharu.kotatsu.parsers.site.ar

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.bitmap.Rect
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser

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

import org.koitharu.kotatsu.parsers.util.json.UmaNextJsData
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.parseJson
import org.koitharu.kotatsu.parsers.util.parseRaw
import org.koitharu.kotatsu.parsers.util.urlEncoded

import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.EnumSet
import java.util.Locale
import java.util.TimeZone

@MangaSourceParser("PROCOMIC", "ProComic", "ar")
internal class ProComic(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.PROCOMIC, 50) {

    override val configKeyDomain = ConfigKey.Domain("procomic.pro", "procomic.net")

    private val baseUrl get() = "https://$domain"
    private val siteHost get() = domain

    private val showLockedKey = ConfigKey.PreferredImageServer(
        presetValues = mapOf(
            "false" to "Hide locked",
            "true" to "Show locked",
        ),
        defaultValue = "false",
    )
    private val showLocked: Boolean get() = config[showLockedKey] == "true"

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(showLockedKey)
    }

    override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
        .set("Referer", "$baseUrl/")
        .set("Origin", baseUrl)
        .set("Cookie", "safe_browsing=off")
        .build()

    private fun htmlHeaders() = getRequestHeaders().newBuilder()
        .set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .build()

    override val availableSortOrders: Set<SortOrder> = EnumSet.of(
        SortOrder.RELEVANCE,
        SortOrder.POPULARITY,
        SortOrder.UPDATED,
        SortOrder.NEWEST,
        SortOrder.ALPHABETICAL,
        SortOrder.ALPHABETICAL_DESC,
    )

    override val filterCapabilities = MangaListFilterCapabilities(
        isSearchSupported = true,
        isSearchWithFiltersSupported = true,
        isMultipleTagsSupported = false,
    )

    override suspend fun getFilterOptions() = MangaListFilterOptions(
        availableTags = emptySet(),
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

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        return if (filter.query.isNullOrBlank()) {
            fetchListing(page) {
                addPathSegments("api/public/content")
                addQueryParameter("sort", sortParam(order))
                contentTypes(filter.types)?.let { addQueryParameter("type", it) }
                mangaStates(filter.states)?.let { addQueryParameter("progress", it) }
            }
        } else {
            fetchListing(page) {
                addPathSegments("api/public/series/search")
                addQueryParameter("search", filter.query!!.trim())
            }
        }
    }

    private suspend fun fetchListing(
        startPage: Int,
        url: HttpUrl.Builder.() -> Unit,
    ): List<Manga> {
        var currentPage = startPage
        var guard = 0
        while (guard++ < 20) {
            val fullUrl = baseUrl.toHttpUrl().newBuilder().apply(url)
                .addQueryParameter("limit", "50")
                .addQueryParameter("page", currentPage.toString())
                .build()
            val raw = webClient.httpGet(fullUrl, getRequestHeaders()).parseRaw()
            val json = JSONObject(raw)
            currentPage++
            val data = json.optJSONArray("data") ?: JSONArray()
            val meta = json.optJSONObject("meta")
            val pageNow = meta?.optInt("page", 1) ?: 1
            val totalPages = meta?.optInt("pages", 1) ?: 1
            val hasNext = pageNow < totalPages

            val mangas = (0 until data.length()).mapNotNull { i ->
                val obj = data.optJSONObject(i) ?: return@mapNotNull null
                val type = obj.optString("type")
                if (type == "novel") return@mapNotNull null
                if (obj.optBoolean("isBlockedSeries", false)) return@mapNotNull null
                parseManga(obj)
            }
            if (mangas.isNotEmpty()) return mangas
            if (!hasNext) return emptyList()
        }
        return emptyList()
    }

    private fun sortParam(order: SortOrder): String = when (order) {
        SortOrder.POPULARITY, SortOrder.POPULARITY_ASC -> "popular"
        SortOrder.UPDATED, SortOrder.UPDATED_ASC -> "latest_chapter"
        SortOrder.NEWEST, SortOrder.NEWEST_ASC -> "latest"
        SortOrder.ALPHABETICAL -> "az"
        SortOrder.ALPHABETICAL_DESC -> "za"
        else -> "popular"
    }

    private fun contentTypes(types: Set<ContentType>): String? {
        if (types.isEmpty()) return null
        return when (types.first()) {
            ContentType.MANGA -> "manga"
            ContentType.MANHWA -> "manhwa"
            ContentType.MANHUA -> "manhua"
            else -> null
        }
    }

    private fun mangaStates(states: Set<MangaState>): String? {
        if (states.isEmpty()) return null
        return when (states.first()) {
            MangaState.ONGOING -> "مستمر"
            MangaState.FINISHED -> "مكتمل"
            MangaState.PAUSED -> "متوقف"
            else -> null
        }
    }

    private fun parseManga(obj: JSONObject): Manga? {
        val id = obj.optInt("id", -1)
        if (id <= 0) return null
        val title = obj.optString("title").ifBlank { return null }
        val slug = obj.optString("slug")
        val cdnPath = obj.optString("cdn_path")
        val thumbRaw = obj.optString("thumbnail").ifBlank { obj.optString("coverImage") }
        val coverUrl = if (thumbRaw.isBlank()) {
            null
        } else if (thumbRaw.startsWith("/")) {
            "https://$cdnPath.$siteHost$thumbRaw"
        } else {
            thumbRaw
        }
        return Manga(
            id = generateUid(id.toString()),
            url = id.toString(),
            publicUrl = "$baseUrl/ar/$slug-$id",
            title = title,
            coverUrl = coverUrl,
            altTitles = emptySet(),
            rating = RATING_UNKNOWN,
            contentRating = null,
            tags = emptySet(),
            state = null,
            authors = emptySet(),
            source = source,
        )
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val id = manga.url.toIntOrNull() ?: return manga
        val json = fetchContent(id) ?: return manga
        val slug = json.optString("slug")
        val type = json.optString("type", "manga")
        val chapters = fetchChapters(type, id.toString(), slug)
        return applyDetails(manga, json).copy(chapters = chapters)
    }

    private suspend fun fetchContent(id: Int): JSONObject? = runCatching {
        JSONObject(webClient.httpGet("$baseUrl/api/public/content/$id", getRequestHeaders()).parseRaw())
    }.getOrNull()

    private fun applyDetails(manga: Manga, json: JSONObject): Manga {
        val title = json.optString("title").ifBlank { manga.title }
        val cdnPath = json.optString("cdn_path")
        val thumbRaw = json.optString("thumbnail").ifBlank { json.optString("coverImage") }
        val coverUrl = if (thumbRaw.isBlank()) manga.coverUrl else if (thumbRaw.startsWith("/"))
            "https://$cdnPath.$siteHost$thumbRaw" else thumbRaw

        val metadata = json.optJSONObject("metadata")
        val author = metadata?.optStringOrList("author")
        val artist = metadata?.optStringOrList("artist")
        val origin = metadata?.optString("origin")
        val year = metadata?.optString("year")

        val altTitles = mutableSetOf<String>()
        metadata?.optJSONArray("altTitles")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optString(i).takeIf { it.isNotBlank() }?.let(altTitles::add)
            }
        }

        val tags = linkedSetOf<String>()
        val type = json.optString("type")
        if (type.isNotBlank()) tags.add(type.replaceFirstChar { it.titlecase(Locale.ROOT) })
        origin?.takeIf { it.isNotBlank() }?.let { tags.add(it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) }) }
        year?.takeIf { it.isNotBlank() }?.let(tags::add)
        metadata?.optJSONArray("genres")?.let { arr ->
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let(tags::add)
        }
        metadata?.optJSONArray("tags")?.let { arr ->
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let(tags::add)
        }

        val state = when (json.optString("progress").trim()) {
            "مستمر" -> MangaState.ONGOING
            "مكتمل" -> MangaState.FINISHED
            "متوقف" -> MangaState.PAUSED
            else -> null
        }

        val description = buildString {
            json.optString("description").takeIf { it.isNotBlank() }?.let {
                append(it.trim())
                if (altTitles.isNotEmpty()) append("\n\n")
            }
            if (altTitles.isNotEmpty()) {
                append("عناوين بديلة\n")
                altTitles.forEach { append("- ").append(it).append('\n') }
            }
        }.trim().ifBlank { null }

        return manga.copy(
            title = title,
            coverUrl = coverUrl,
            authors = setOfNotNull(author, artist).filter { it.isNotBlank() }.toSet(),
            description = description,
            tags = tags.map { MangaTag(it, it, source) }.toSet(),
            state = state,
        )
    }

    private fun JSONObject.optStringOrList(key: String): String? {
        val raw = opt(key) ?: return null
        return when (raw) {
            is String -> raw.takeIf { it.isNotBlank() }
            is JSONArray -> (0 until raw.length())
                .mapNotNull { raw.optString(it).takeIf { s -> s.isNotBlank() } }
                .joinToString(", ").ifBlank { null }
            else -> null
        }
    }

    private suspend fun fetchChapters(type: String, mangaId: String, slug: String): List<MangaChapter> {
        val chapters = mutableListOf<MangaChapter>()
        var page = 1
        while (page <= 100) {
            val url = "$baseUrl/api/public/$type/$mangaId/chapters?limit=100&order=desc&page=$page"
            val json = runCatching {
                JSONObject(webClient.httpGet(url, getRequestHeaders()).parseRaw())
            }.getOrNull() ?: break
            val data = json.optJSONArray("data") ?: break
            val total = json.optInt("total", data.length())
            for (i in 0 until data.length()) {
                val ch = data.optJSONObject(i) ?: continue
                if (ch.optString("language") != "AR") continue
                if (!showLocked && isLocked(ch)) continue
                chapters.add(parseChapter(ch, slug, mangaId))
            }
            if (chapters.size >= total) break
            if (data.length() == 0) break
            page++
        }
        return chapters.sortedBy { it.number }
    }

    private fun isLocked(ch: JSONObject): Boolean = listOf(
        "lockedForever",
        "lockedByCoins",
        "lockedByShortlink",
        "lockedByExclusive",
    ).any { ch.optBoolean(it, false) }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parseChapter(ch: JSONObject, slug: String, mangaId: String): MangaChapter {
        val id = ch.optInt("id", -1)
        val number = ch.optString("chapter_number")
        val title = ch.optString("title").takeIf { it.isNotBlank() && it != number }
        val publishedAt = ch.optString("published_at")
        val uploadDate = runCatching {
            dateFormat.parse(publishedAt.take(19))?.time ?: 0L
        }.getOrDefault(0L)
        val locked = isLocked(ch)

        val displayName = buildString {
            if (locked) append("🔒 ")
            append("الفصل ").append(number)
            title?.let { append(" - ").append(it) }
        }

        return MangaChapter(
            id = generateUid("$mangaId/$id"),
            title = displayName,
            number = number.toFloatOrNull() ?: 0f,
            volume = 0,
            url = "$mangaId/$id/$slug/$number",
            uploadDate = uploadDate,
            source = source,
            scanlator = ch.optString("translator").takeIf { it.isNotBlank() },
            branch = null,
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val parts = chapter.url.split("/")
        if (parts.size < 4) return emptyList()

        val chapterId = parts[1].toIntOrNull() ?: return emptyList()
        val slug = parts[2]
        val number = parts[3]

        val readerUrl = "https://$domain/ar/chapter/$slug-$number-$chapterId"
        val html = runCatching {
            webClient.httpGet(readerUrl, htmlHeaders()).parseRaw()
        }.getOrNull() ?: return emptyList()

        val reader = UmaNextJsData.findValue(html) { node ->
            node is JSONObject &&
                    node.has("chapterId") &&
                    node.has("appImages") &&
                    node.has("deferredMedia")
        } as? JSONObject ?: return emptyList()

        val readerChapterId = reader.optInt("chapterId", chapterId)
        val cdnPath = reader.optString("cdnPath")

        val images = mutableListOf<String>()
        reader.optJSONArray("appImages")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val url = obj.optString("mobile").ifBlank { obj.optString("desktop") }
                if (url.isNotBlank()) images.add(url)
            }
        }

        val deferredImages = mutableListOf<String>()
        val mapRequests = mutableListOf<String>()

        val deferredToken = reader.optJSONObject("deferredMedia")?.optString("token").orEmpty()
        if (deferredToken.isNotBlank()) {
            val deferredUrl =
                "https://$domain/chapter-deferred-media/$readerChapterId?token=${deferredToken.urlEncoded()}"
            val response = runCatching {
                webClient.httpGet(deferredUrl, getRequestHeaders()).parseJson()
            }.getOrNull()

            response?.optJSONObject("data")?.let { data ->
                data.optJSONArray("images")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        arr.optString(i).takeIf { it.isNotBlank() }?.let(deferredImages::add)
                    }
                }
                data.optJSONArray("maps")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val entry = arr.optJSONObject(i) ?: continue
                        val plan = JSONObject().apply {
                            put("chapterId", readerChapterId)
                            put("token", entry.optString("token"))
                            put("method", entry.optString("method"))
                            put("cdnPath", cdnPath)
                            put("pageIndex", images.size + deferredImages.size + i)
                        }
                        mapRequests.add("http://procomic.pieces#${plan}")
                    }
                }
            }
        }

        val deferredUrls = deferredImages.map { "http://procomic.signed?url=${it.urlEncoded()}" }

        return (images + deferredUrls + mapRequests).mapIndexed { index, url ->
            MangaPage(
                id = generateUid("${chapter.id}:page:$index"),
                url = url,
                preview = null,
                source = source,
            )
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        return when (url.host) {
            PIECE_HOST, SIGNED_IMAGE_HOST -> try {
                if (url.host == PIECE_HOST) stitchResponse(request, url, chain) else signAndRedirect(request, url, chain)
            } catch (error: Exception) {
                if (chain.call().isCanceled() || Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException("Image request cancelled")
                errorResponse(request, error)
            }
            else -> chain.proceed(request)
        }
    }

    private fun errorResponse(request: Request, cause: Throwable): Response =
        Response.Builder()
            .request(request.newBuilder().url(request.url.newBuilder().fragment(null).query(null).build()).build())
            .protocol(Protocol.HTTP_1_1)
            .code(502)
            .message(cause.message ?: "procomic proxy failed")
            .body("".toResponseBody(null))
            .build()

    private fun signAndRedirect(
        request: Request,
        url: HttpUrl,
        chain: Interceptor.Chain,
    ): Response {
        val image = url.queryParameter("url") ?: throw IOException("Signed image address missing")
        if (!image.toHttpUrl().isHttps) throw IOException("Unsafe signed image address")
        val signed = signImage(image, chain.call())
        val signedUrl = "$baseUrl/api/cdn-image".toHttpUrl().newBuilder()
            .addQueryParameter("url", image)
            .addQueryParameter("token", signed.first)
            .addQueryParameter("expires", signed.second.toString())
            .build()
        return chain.proceed(request.newBuilder().url(signedUrl).build())
    }

    private fun signImage(image: String, parent: Call): Pair<String, Long> {
        val url = "$baseUrl/api/cdn-image/sign"
        val body = JSONObject().put("url", image)
        val req = Request.Builder()
            .url(url)
            .headers(getRequestHeaders())
            .tag(org.koitharu.kotatsu.parsers.model.MangaSource::class.java, source)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        executeInternal(req, parent).use { resp ->
            if (!resp.isSuccessful) throw IOException("Sign failed ${resp.code}")
            val json = JSONObject(resp.body?.string() ?: "")
            return json.optString("token") to json.optLong("expires", 0L)
        }
    }

    private fun stitchResponse(
        request: Request,
        url: HttpUrl,
        chain: Interceptor.Chain,
    ): Response {
        val planData = url.fragment ?: throw IOException("Image plan missing")
        if (planData.length > 262_144) throw IOException("Image plan too large")
        val acquired = try { STITCH_PERMIT.tryAcquire(15, java.util.concurrent.TimeUnit.SECONDS) }
            catch (error: InterruptedException) { Thread.currentThread().interrupt(); throw java.io.InterruptedIOException("Image request cancelled") }
        if (!acquired) throw IOException("Image stitching is busy; retry this page")
        return try { stitch(planData, chain.call()) } finally { STITCH_PERMIT.release() }
    }
    private fun executeInternal(request: Request, parent: Call): Response {
        if (parent.isCanceled() || Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException("Image request cancelled")
        val nested = context.httpClient.newCall(request)
        nested.timeout().timeout(15, java.util.concurrent.TimeUnit.SECONDS)
        return nested.execute().also { response ->
            if (parent.isCanceled() || Thread.currentThread().isInterrupted) {
                response.close()
                throw java.io.InterruptedIOException("Image request cancelled")
            }
        }
    }

    private fun stitch(planJson: String, call: Call): Response {
        val plan = JSONObject(planJson)
        val chapterId = plan.optInt("chapterId", -1)
        if (chapterId <= 0) throw IOException("Map plan missing chapterId")

        val body = JSONObject().apply {
            put("token", plan.optString("token"))
            put("method", plan.optString("method"))
            put("cdnPath", plan.optString("cdnPath"))
            put("pageIndex", plan.optInt("pageIndex"))
        }
        val url = "$baseUrl/chapter-map-proxy-plan/$chapterId"
        val req = Request.Builder()
            .url(url)
            .headers(getRequestHeaders())
            .tag(org.koitharu.kotatsu.parsers.model.MangaSource::class.java, source)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val planResp = executeInternal(req, call).use { resp ->
            if (!resp.isSuccessful) throw IOException("Map plan failed ${resp.code}")
            val bodySource = resp.body?.source() ?: throw IOException("Image plan response missing")
            bodySource.request(262_145)
            if (bodySource.buffer.size > 262_144) throw IOException("Image plan response too large")
            JSONObject(bodySource.readUtf8())
        }
        val map = planResp.optJSONObject("data")?.optJSONObject("map")
            ?: throw IOException("Map plan missing")
        val layout = parseProComicMap(map)
        if (call.isCanceled()) throw java.io.InterruptedIOException("Image request cancelled")
        val finalBitmap = context.createBitmap(layout.width, layout.height)
        var transferred = false
        try {
            for (piece in layout.pieces) {
                if (call.isCanceled() || Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException("Image request cancelled")
                val pieceReq = Request.Builder().url(piece.url).headers(getRequestHeaders())
                    .tag(org.koitharu.kotatsu.parsers.model.MangaSource::class.java, source).build()
                executeInternal(pieceReq, call).use { pieceResponse ->
                    if (!pieceResponse.isSuccessful) throw IOException("Image piece failed: HTTP ${pieceResponse.code}")
                    context.redrawImageResponse(pieceResponse) { pieceBitmap ->
                        if (call.isCanceled()) throw java.io.InterruptedIOException("Image request cancelled")
                        finalBitmap.drawBitmap(pieceBitmap, Rect(0, 0, pieceBitmap.width, pieceBitmap.height),
                            Rect(piece.left, piece.top, piece.left + piece.width, piece.top + piece.height))
                        pieceBitmap
                    }.close()
                }
            }
            if (call.isCanceled()) throw java.io.InterruptedIOException("Image request cancelled")
            val baseReq = Request.Builder().url(layout.pieces.first().url).headers(getRequestHeaders())
                .tag(org.koitharu.kotatsu.parsers.model.MangaSource::class.java, source).build()
            return executeInternal(baseReq, call).use { baseResponse ->
                if (!baseResponse.isSuccessful) throw IOException("Image encoding failed: HTTP ${baseResponse.code}")
                context.redrawImageResponse(baseResponse) { transferred = true; finalBitmap }
            }
        } finally {
            if (!transferred) (finalBitmap as? AutoCloseable)?.close()
        }
    }

    override suspend fun getRelatedManga(seed: Manga): List<Manga> = emptyList()

    companion object {
        private val STITCH_PERMIT = java.util.concurrent.Semaphore(1)
        private const val PIECE_HOST = "procomic.pieces"
        private const val SIGNED_IMAGE_HOST = "procomic.signed"
    }
}

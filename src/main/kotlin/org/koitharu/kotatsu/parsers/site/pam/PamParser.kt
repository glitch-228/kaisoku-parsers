package org.koitharu.kotatsu.parsers.site.pam

import androidx.annotation.VisibleForTesting
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.Request
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.json.JSONObject
import org.jsoup.Jsoup
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
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
import org.koitharu.kotatsu.parsers.util.getCookies
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.json.mapJSONToSet
import org.koitharu.kotatsu.parsers.util.secretstream.SecretStream
import org.koitharu.kotatsu.parsers.util.secretstream.State
import org.koitharu.kotatsu.parsers.util.secretstream.X25519
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.EnumSet
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Base class for the "PAM" sites — a Laravel + Inertia.js stack whose reader hands out encrypted
 * pages instead of image URLs. Named after the `/wasm/pam.js` module the site ships.
 *
 * Reading a chapter is a three-step exchange:
 * 1. The chapter page carries an X25519 `server_pubkey` and a `chapter_token`. We generate an
 *    ephemeral keypair and derive a shared secret from the server key.
 * 2. Each page is requested from `/serie/<serie>/chapter/<chapter>/page/<i>` with the token, a
 *    timestamp, a random nonce and `HMAC-SHA256(chapter_token, "<page><ts><nonce>")`, plus our
 *    public key in `X-Client-Pubkey`.
 * 3. The response carries `X-Page-Name` and `X-Key-Hint`. The stream key is
 *    `SHA-256(sharedSecret || pageName) XOR keyHint`, and the body is 192 bytes of padding, a
 *    24-byte libsodium secretstream header, then XChaCha20-Poly1305 chunks.
 *
 * The site also decrypts in WebAssembly and paints to `<canvas>`, but that is only how *it* renders
 * the result — none of the WASM, WebGL attestation or captcha material is needed to fetch and
 * decrypt a page.
 */
internal abstract class PamParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : PagedMangaParser(context, source, pageSize = 24) {

	override val configKeyDomain = ConfigKey.Domain(defaultDomain)

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.NEWEST,
		SortOrder.ALPHABETICAL,
	)

	override val filterCapabilities = MangaListFilterCapabilities(
		isMultipleTagsSupported = true,
		isTagsExclusionSupported = true,
		isSearchSupported = true,
	)

	/** Genre slugs offered by the site, as `title to slug`. */
	protected abstract val genres: List<Pair<String, String>>

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override fun getRequestHeaders(): Headers = super.getRequestHeaders().newBuilder()
		.set("Origin", "https://$domain")
		.set("Referer", "https://$domain/")
		.build()

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = genres.mapTo(LinkedHashSet(genres.size)) { (title, slug) ->
			MangaTag(title = title, key = slug, source = source)
		},
		availableStates = EnumSet.of(
			MangaState.ONGOING,
			MangaState.FINISHED,
			MangaState.ABANDONED,
			MangaState.PAUSED,
			MangaState.UPCOMING,
		),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (!filter.query.isNullOrEmpty()) {
			// Search ignores every other filter and is not paginated.
			if (page > 1) {
				return emptyList()
			}
			val url = urlBuilder()
				.addPathSegments("api/v1/search/series")
				.addQueryParameter("q", filter.query)
				.build()
			return fetchJson(url, isInertia = false)
				.optJSONArray("data")
				?.mapJSON { it.toManga() }
				?: emptyList()
		}
		val url = urlBuilder().apply {
			addPathSegment("library")
			if (page > 1) {
				addQueryParameter("page", page.toString())
			}
			filter.tags.takeIf { it.isNotEmpty() }?.let {
				addQueryParameter("include_genres", it.joinToString(",") { tag -> tag.key })
			}
			filter.tagsExclude.takeIf { it.isNotEmpty() }?.let {
				addQueryParameter("exclude_genres", it.joinToString(",") { tag -> tag.key })
			}
			filter.states.takeIf { it.isNotEmpty() }?.let { states ->
				val values = states.mapNotNull { state ->
					when (state) {
						MangaState.ONGOING -> "ongoing"
						MangaState.FINISHED -> "finished"
						MangaState.ABANDONED -> "dropped"
						MangaState.PAUSED -> "onhold"
						MangaState.UPCOMING -> "upcoming"
						else -> null
					}
				}
				if (values.isNotEmpty()) {
					addQueryParameter("status", values.joinToString(","))
				}
			}
			addQueryParameter(
				"orderby",
				when (order) {
					SortOrder.POPULARITY -> "views"
					SortOrder.NEWEST -> "date"
					SortOrder.ALPHABETICAL -> "alphabetical"
					else -> "recently"
				},
			)
		}.build()
		// The library answers with a bare `{series: {data, meta}}` payload, but falls back to the
		// full Inertia envelope when it is served as HTML.
		return fetchJson(url, isInertia = false).unwrapProps()
			.optJSONObject("series")
			?.optJSONArray("data")
			?.mapJSON { it.toManga() }
			?: emptyList()
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val props = fetchProps(manga.url.toAbsoluteUrl(domain).toHttpUrlOrFail())
		val serie = props.optJSONObject("serie") ?: throw ParseException("Series data not found", manga.publicUrl)
		val serieSlug = serie.getString("slug")
		val chapters = serie.optJSONArray("chapters")?.mapJSON { chapter ->
			val slug = chapter.getString("slug")
			MangaChapter(
				id = generateUid("/serie/$serieSlug/chapter/$slug"),
				title = chapter.getStringOrNull("title"),
				number = chapter.optDouble("chapterNumber", 0.0).toFloat(),
				volume = 0,
				url = "/serie/$serieSlug/chapter/$slug",
				scanlator = null,
				uploadDate = parseChapterDate(chapter.getStringOrNull("createdAt")),
				branch = null,
				source = source,
			)
		}.orEmpty() // already oldest-first, which is the order Kotatsu expects
		return manga.copy(
			title = serie.getStringOrNull("name") ?: manga.title,
			altTitles = setOfNotNull(serie.getStringOrNull("name_alternative")),
			description = serie.getStringOrNull("description"),
			authors = setOfNotNull(serie.getStringOrNull("author"), serie.getStringOrNull("artist")),
			state = serie.getStringOrNull("status").toMangaState(),
			coverUrl = serie.getStringOrNull("cover_image")?.toAbsoluteUrl(domain),
			tags = serie.optJSONArray("genres")?.mapJSONToSet { genre ->
				MangaTag(title = genre.getString("name"), key = genre.getString("slug"), source = source)
			}.orEmpty(),
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val props = fetchProps(chapter.url.toAbsoluteUrl(domain).toHttpUrlOrFail())
		val data = props.optJSONObject("data") ?: throw ParseException("Chapter data not found", chapter.url.toAbsoluteUrl(domain))
		val serieSlug = data.getJSONObject("serie").getString("slug")
		val chapterSlug = data.getString("slug")
		val pageCount = props.optInt("page_count", data.optInt("page_count", 0))
		if (pageCount <= 0) {
			throw ParseException("Chapter has no pages", chapter.url.toAbsoluteUrl(domain))
		}
		val sessionId = sessionKey(serieSlug, chapterSlug)
		sessions[sessionId] = handshake(props)
		return (1..pageCount).map { i ->
			MangaPage(
				id = generateUid("$serieSlug/$chapterSlug/$i"),
				// The fragment is dropped before the request is sent; it is how intercept() finds the session.
				url = "https://$domain/serie/$serieSlug/chapter/$chapterSlug/page/$i#$sessionId",
				preview = null,
				source = source,
			)
		}
	}

	// region Inertia

	/**
	 * The Inertia asset version, harvested from any page we load. Sending a stale one makes the
	 * server answer 409, so it is refreshed from every response and dropped on mismatch.
	 */
	@Volatile
	private var inertiaVersion: String? = null

	private fun urlBuilder() = HttpUrl.Builder().scheme("https").host(domain)

	private fun String.toHttpUrlOrFail(): HttpUrl =
		toHttpUrlOrNull() ?: throw ParseException("Invalid url", this)

	private fun apiHeaders(isInertia: Boolean): Headers = getRequestHeaders().newBuilder().apply {
		set("Accept", "application/json, text/html;q=0.9")
		set("X-Requested-With", "XMLHttpRequest")
		context.cookieJar.getCookies(domain)
			.firstOrNull { it.name == "XSRF-TOKEN" }
			?.let { set("X-XSRF-TOKEN", it.value) }
		val version = inertiaVersion
		if (isInertia && version != null) {
			set("X-Inertia", "true")
			set("X-Inertia-Version", version)
		}
	}.build()

	/**
	 * Loads a page and returns its Inertia envelope. The site answers the same
	 * `{component, props, version}` object either as JSON (when asked with the Inertia headers) or
	 * embedded in `#app[data-page]`, so both shapes are accepted — that also bootstraps
	 * [inertiaVersion] on the first request, when we do not have one yet.
	 */
	private suspend fun fetchJson(url: HttpUrl, isInertia: Boolean): JSONObject {
		val body = webClient.httpGet(url, apiHeaders(isInertia)).use { it.body.string() }
		val envelope = body.trimStart().let { trimmed ->
			if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
				JSONObject(if (trimmed.startsWith('[')) "{\"data\":$trimmed}" else trimmed)
			} else {
				val dataPage = Jsoup.parse(body, url.toString())
					.selectFirst("#app")
					?.attr("data-page")
					?: throw ParseException("Page state not found", url.toString())
				JSONObject(dataPage)
			}
		}
		envelope.getStringOrNull("version")?.let { inertiaVersion = it }
		return envelope
	}

	private suspend fun fetchProps(url: HttpUrl): JSONObject {
		val envelope = try {
			fetchJson(url, isInertia = true)
		} catch (e: IOException) {
			// A stale asset version is answered with 409; dropping it makes the retry ask for plain
			// HTML, which carries a fresh version for subsequent requests.
			inertiaVersion = null
			fetchJson(url, isInertia = true)
		}
		return envelope.unwrapProps()
	}

	private fun JSONObject.unwrapProps(): JSONObject = optJSONObject("props") ?: this

	private fun JSONObject.toManga(): Manga {
		val slug = getString("slug")
		val url = "/serie/$slug"
		val cover = getStringOrNull("cover_image") ?: getStringOrNull("image")
		return Manga(
			id = generateUid(url),
			title = getStringOrNull("name") ?: getStringOrNull("title").orEmpty(),
			altTitles = emptySet(),
			url = url,
			publicUrl = url.toAbsoluteUrl(domain),
			rating = RATING_UNKNOWN,
			contentRating = null,
			coverUrl = cover?.toAbsoluteUrl(domain),
			tags = emptySet(),
			state = getStringOrNull("serie_status").toMangaState(),
			authors = emptySet(),
			source = source,
		)
	}

	private fun String?.toMangaState() = when (this?.lowercase()) {
		"ongoing" -> MangaState.ONGOING
		"finished" -> MangaState.FINISHED
		"dropped" -> MangaState.ABANDONED
		"onhold" -> MangaState.PAUSED
		"upcoming" -> MangaState.UPCOMING
		else -> null
	}

	private fun parseChapterDate(value: String?): Long {
		if (value.isNullOrEmpty()) {
			return 0L
		}
		return runCatching {
			chapterDateFormat.parse(value.substringBefore('.'))?.time ?: 0L
		}.getOrDefault(0L)
	}

	// endregion

	// region Page decryption

	private class ChapterSession(
		val chapterToken: String,
		val sharedSecret: ByteArray,
		val clientPubkeyB64: String,
	)

	private val sessions = ConcurrentHashMap<String, ChapterSession>()
	private val secureRandom = SecureRandom()

	private fun sessionKey(serieSlug: String, chapterSlug: String) = "$serieSlug--$chapterSlug"

	private fun handshake(props: JSONObject): ChapterSession {
		val serverPub = Base64.getDecoder().decode(props.getString("server_pubkey"))
		if (serverPub.size != PUBLIC_KEY_LENGTH) {
			throw IOException("Server public key must be $PUBLIC_KEY_LENGTH bytes, got ${serverPub.size}")
		}
		val priv = ByteArray(PUBLIC_KEY_LENGTH).also(secureRandom::nextBytes)
		return try {
			ChapterSession(
				chapterToken = props.getString("chapter_token"),
				sharedSecret = X25519.scalarMult(priv, serverPub),
				clientPubkeyB64 = Base64.getEncoder().encodeToString(X25519.publicKey(priv)),
			)
		} finally {
			priv.fill(0)
		}
	}

	/**
	 * Rebuilds a session that this process never created — Kotatsu stores page urls, so a download
	 * resumed after a restart requests images without going through [getPages] first.
	 */
	private fun restoreSession(sessionId: String, url: HttpUrl): ChapterSession? {
		val segments = url.pathSegments
		val chapterUrl = urlBuilder()
			.addPathSegment("serie").addPathSegment(segments[1])
			.addPathSegment("chapter").addPathSegment(segments[3])
			.build()
		val request = Request.Builder()
			.url(chapterUrl)
			.headers(apiHeaders(isInertia = false))
			.build()
		val envelope = context.httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				return null
			}
			val body = response.body.string()
			val dataPage = Jsoup.parse(body, chapterUrl.toString()).selectFirst("#app")?.attr("data-page")
			JSONObject(dataPage ?: body)
		}
		val props = envelope.optJSONObject("props") ?: envelope
		if (!props.has("server_pubkey") || !props.has("chapter_token")) {
			return null
		}
		return handshake(props).also { sessions[sessionId] = it }
	}

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val sessionId = request.url.fragment
		if (sessionId == null || !request.url.encodedPath.contains("/page/")) {
			return chain.proceed(request)
		}
		val session = sessions[sessionId]
			?: restoreSession(sessionId, request.url)
			?: return chain.proceed(request)
		val pageIndex = request.url.pathSegments.last()
		val timestamp = (System.currentTimeMillis() / 1000L).toString()
		val nonce = ByteArray(NONCE_LENGTH).also(secureRandom::nextBytes).toHexString()
		val signedRequest = request.newBuilder()
			.url(
				request.url.newBuilder()
					.addQueryParameter("token", session.chapterToken)
					.addQueryParameter("ts", timestamp)
					.addQueryParameter("nonce", nonce)
					.addQueryParameter("sig", sign(session.chapterToken, "$pageIndex$timestamp$nonce"))
					.build(),
			)
			.header("X-Client-Pubkey", session.clientPubkeyB64)
			.header("Accept", "application/octet-stream")
			.build()
		val response = chain.proceed(signedRequest)
		if (!response.isSuccessful) {
			return response
		}
		val pageName = response.header("X-Page-Name") ?: return response
		val keyHint = Base64.getDecoder().decode(response.header("X-Key-Hint") ?: return response)
		if (keyHint.size < STREAM_KEY_LENGTH) {
			response.close()
			throw IOException("Key hint must be at least $STREAM_KEY_LENGTH bytes, got ${keyHint.size}")
		}
		val streamKey = deriveStreamKey(session.sharedSecret, pageName, keyHint)
		val body = response.body
		val source = body.source()
		source.skip(PREFIX_LENGTH)
		val header = source.readByteArray(STREAM_HEADER_LENGTH)
		return response.newBuilder()
			.removeHeader("Content-Length")
			.body(
				SecretStreamSource(source, header, streamKey)
					.buffer()
					.asResponseBody(pageName.toImageMediaType()),
			)
			.build()
	}

	/** Decrypts the libsodium secretstream lazily so a page is never buffered whole. */
	@VisibleForTesting
	internal class SecretStreamSource(
		private val upstream: BufferedSource,
		header: ByteArray,
		streamKey: ByteArray,
		/** Size of one encrypted chunk; only tests use anything but the default. */
		private val chunkSize: Long = CHUNK_SIZE,
	) : Source {

		private val secretStream = SecretStream()
		private val state = State().also { secretStream.initPull(it, header, streamKey) }
		private val decrypted = Buffer()
		private var isFinished = false

		override fun read(sink: Buffer, byteCount: Long): Long {
			if (decrypted.size == 0L) {
				if (isFinished) {
					return -1L
				}
				upstream.request(chunkSize)
				val available = minOf(chunkSize, upstream.buffer.size)
				if (available == 0L) {
					isFinished = true
					return -1L
				}
				val chunk = Buffer().apply { upstream.read(this, available) }.readByteArray()
				val result = secretStream.pull(state, chunk, chunk.size)
					?: throw IOException("Page decryption failed")
				decrypted.write(result.message)
				if (result.tag.toInt() == SecretStream.TAG_FINAL) {
					isFinished = true
				}
			}
			return decrypted.read(sink, byteCount)
		}

		override fun timeout(): Timeout = upstream.timeout()

		override fun close() = upstream.close()
	}

	internal companion object {

		private const val PUBLIC_KEY_LENGTH = 32
		private const val STREAM_KEY_LENGTH = 32
		private const val NONCE_LENGTH = 16

		/** Junk the server prepends to the encrypted body. */
		private const val PREFIX_LENGTH = 192L
		private const val STREAM_HEADER_LENGTH = 24L

		/** libsodium secretstream message limit plus its per-chunk overhead. */
		private const val CHUNK_SIZE = 65536L + SecretStream.ABYTES

		private val chapterDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
			timeZone = TimeZone.getTimeZone("UTC")
		}

		@VisibleForTesting
		fun deriveStreamKey(sharedSecret: ByteArray, pageName: String, keyHint: ByteArray): ByteArray {
			val digest = MessageDigest.getInstance("SHA-256").run {
				update(sharedSecret)
				update(pageName.toByteArray(Charsets.UTF_8))
				digest()
			}
			return ByteArray(STREAM_KEY_LENGTH) { i -> (digest[i].toInt() xor keyHint[i].toInt()).toByte() }
		}

		@VisibleForTesting
		fun sign(key: String, message: String): String {
			val mac = Mac.getInstance("HmacSHA256").apply {
				init(SecretKeySpec(key.toByteArray(Charsets.US_ASCII), "HmacSHA256"))
			}
			return mac.doFinal(message.toByteArray(Charsets.US_ASCII)).toHexString()
		}

		fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

		fun String.toImageMediaType() = when {
			endsWith(".webp", ignoreCase = true) -> "image/webp"
			endsWith(".png", ignoreCase = true) -> "image/png"
			else -> "image/jpeg"
		}.toMediaTypeOrNull()
	}

	// endregion
}

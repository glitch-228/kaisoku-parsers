package org.koitharu.kotatsu.parsers.site.id

import okhttp3.Headers
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.getDoubleOrDefault
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNullToSet
import org.koitharu.kotatsu.parsers.util.json.mapJSONToSet
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@MangaSourceParser("DOUJINDESU", "Doujindesu.XXX", "id", type = ContentType.HENTAI)
internal class DoujinDesuParser(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.DOUJINDESU, pageSize = 24) {

	override val configKeyDomain: ConfigKey.Domain
		get() = ConfigKey.Domain("doujindesu.tv", "doujindesu.xxx", "doujin.desu.xxx")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override val availableSortOrders: Set<SortOrder>
		get() = EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY, SortOrder.RATING)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isMultipleTagsSupported = true,
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = fetchAvailableTags(),
		availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED),
		availableContentTypes = EnumSet.of(
			ContentType.MANGA,
			ContentType.MANHWA,
			ContentType.DOUJINSHI,
		),
	)

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("X-App-Secret", APP_SECRET)
		.add("Referer", "https://$domain/")
		.add("User-Agent", config[userAgentKey])
		.build()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = urlBuilder().apply {
			addPathSegment("api")
			addPathSegment("manga")
			addQueryParameter("limit", pageSize.toString())
			addQueryParameter("offset", ((page - 1) * pageSize).toString())
			addQueryParameter(
				"sort",
				when (order) {
					SortOrder.POPULARITY -> "views"
					SortOrder.RATING -> "rating"
					else -> "latest_chapter"
				},
			)
			filter.query?.takeIf { it.isNotBlank() }?.let { addQueryParameter("search", it) }
			if (filter.tags.isNotEmpty()) {
				addQueryParameter("genre", filter.tags.joinToString(",") { it.key })
			}
			filter.states.oneOrThrowIfMany()?.let {
				addQueryParameter(
					"status",
					when (it) {
						MangaState.ONGOING -> "ongoing"
						MangaState.FINISHED -> "completed"
						else -> ""
					},
				)
			}
			filter.types.oneOrThrowIfMany()?.let {
				addQueryParameter(
					"type",
					when (it) {
						ContentType.MANGA -> "manga"
						ContentType.MANHWA -> "manhwa"
						ContentType.DOUJINSHI -> "doujinshi"
						else -> ""
					},
				)
			}
		}.build()
		return JSONArray(apiRequest(url)).mapJSON(::parseManga)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val slug = manga.url.substringAfterLast('/')
		val url = urlBuilder().addPathSegment("api").addPathSegment("manga").addPathSegment(slug).build()
		val obj = JSONObject(apiRequest(url))
		val chapters = obj.optJSONArray("chapters")
			?.mapJSON { it }
			?.sortedBy { it.getDoubleOrDefault("chapter_number", 0.0) }
			.orEmpty()
		return parseManga(obj).copy(
			description = obj.getStringOrNull("description"),
			tags = obj.optJSONArray("manga_genres").parseGenres(),
			chapters = chapters.mapIndexed { index, jo ->
				val chapterId = jo.getString("id")
				val number = jo.getDoubleOrDefault("chapter_number", (index + 1).toDouble()).toFloat()
				MangaChapter(
					id = generateUid(chapterId),
					title = jo.getStringOrNull("title"),
					number = number,
					volume = 0,
					url = "/chapters/$chapterId",
					scanlator = null,
					uploadDate = chapterDateFormat.parseSafe(jo.getStringOrNull("created_at")),
					branch = null,
					source = source,
				)
			},
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val id = chapter.url.substringAfterLast('/')
		val url = urlBuilder().addPathSegment("api").addPathSegment("chapters").addPathSegment(id).build()
		val images = JSONObject(apiRequest(url)).optJSONArray("content_urls") ?: return emptyList()
		return (0 until images.length()).map { i ->
			val imageUrl = images.getString(i).replace(" ", "%20")
			MangaPage(
				id = generateUid(imageUrl),
				url = imageUrl,
				preview = null,
				source = source,
			)
		}
	}

	private fun parseManga(jo: JSONObject): Manga {
		val slug = jo.getString("slug")
		val author = jo.getStringOrNull("author")
		val rating = jo.getDoubleOrDefault("rating", -1.0)
		return Manga(
			id = generateUid(slug),
			title = jo.getStringOrNull("title").orEmpty(),
			altTitles = jo.getStringOrNull("alt_titles")
				?.split('|')
				?.mapNotNullToSet { it.trim().nullIfEmpty() }
				.orEmpty(),
			url = "/manga/$slug",
			publicUrl = "https://$domain/manga/$slug",
			rating = if (rating >= 0) (rating / 10.0).toFloat() else RATING_UNKNOWN,
			contentRating = ContentRating.ADULT,
			coverUrl = jo.getStringOrNull("cover_url"),
			tags = jo.optJSONArray("manga_genres").parseGenres(),
			state = when (jo.getStringOrNull("status")?.lowercase()) {
				"completed" -> MangaState.FINISHED
				"ongoing" -> MangaState.ONGOING
				else -> null
			},
			authors = setOfNotNull(author),
			source = source,
		)
	}

	private fun JSONArray?.parseGenres(): Set<MangaTag> {
		if (this == null) return emptySet()
		return mapJSONNotNullToSet { entry ->
			val genre = entry.optJSONObject("genres") ?: entry
			val key = genre.getStringOrNull("slug").orEmpty()
			if (key.isEmpty()) {
				return@mapJSONNotNullToSet null
			}
			MangaTag(
				key = key,
				title = genre.getStringOrNull("name").orEmpty().toTitleCase(sourceLocale),
				source = source,
			)
		}
	}

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		val url = urlBuilder().addPathSegment("api").addPathSegment("genres").build()
		return JSONArray(apiRequest(url)).mapJSONToSet { jo ->
			MangaTag(
				key = jo.getStringOrNull("slug").orEmpty(),
				title = jo.getStringOrNull("name").orEmpty().toTitleCase(sourceLocale),
				source = source,
			)
		}
	}

	/**
	 * The site serves API responses encrypted as `{"_enc_resp_":"<hex>"}` and gates them behind
	 * the [APP_SECRET] header (see [getRequestHeaders]). Returns the decrypted JSON text, or the raw
	 * body when a response is not encrypted.
	 */
	private suspend fun apiRequest(url: HttpUrl): String {
		val raw = webClient.httpGet(url).parseRaw()
		val enc = runCatching { JSONObject(raw).getStringOrNull(ENC_FIELD) }.getOrNull()
		return if (!enc.isNullOrEmpty()) decrypt(enc) else raw
	}

	private fun decrypt(hex: String): String {
		val bucket = System.currentTimeMillis() / KEY_WINDOW_MS
		for (candidate in longArrayOf(bucket, bucket - 1, bucket + 1)) {
			val decoded = runCatching {
				decodeUriComponent(xorDecode(hex, deriveKey(candidate)))
			}.getOrNull()
			if (decoded != null && (decoded.startsWith("{") || decoded.startsWith("["))) {
				return decoded
			}
		}
		throw ParseException("Unable to decrypt API response", "https://$domain/")
	}

	private fun deriveKey(bucket: Long): String {
		var n = 0
		for (ch in "${SECRET_SALT}_$bucket") {
			n = (n shl 5) - n + ch.code
		}
		var l = abs(n.toLong()).takeIf { it != 0L } ?: 123456789L
		return buildString(KEY_LENGTH) {
			repeat(KEY_LENGTH) {
				l = (l * 1664525L + 1013904223L) % 4294967296L
				append((33 + (l % 93)).toInt().toChar())
			}
		}
	}

	private fun xorDecode(hex: String, key: String): ByteArray {
		val out = ByteArray(hex.length / 2)
		var d = 42
		for (i in out.indices) {
			val p = hex.substring(i * 2, i * 2 + 2).toInt(16)
			out[i] = ((p xor key[i % key.length].code xor (i * 13) xor d) and 0xFF).toByte()
			d = (d + p) % 256
		}
		return out
	}

	private fun decodeUriComponent(bytes: ByteArray): String {
		val buffer = ByteArrayOutputStream(bytes.size)
		var i = 0
		while (i < bytes.size) {
			val b = bytes[i].toInt() and 0xFF
			if (b == '%'.code && i + 2 < bytes.size) {
				val hi = Character.digit(bytes[i + 1].toInt().toChar(), 16)
				val lo = Character.digit(bytes[i + 2].toInt().toChar(), 16)
				if (hi >= 0 && lo >= 0) {
					buffer.write((hi shl 4) or lo)
					i += 3
					continue
				}
			}
			buffer.write(b)
			i++
		}
		return buffer.toString(Charsets.UTF_8.name())
	}

	private val chapterDateFormat get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
		timeZone = TimeZone.getTimeZone("UTC")
	}

	private companion object {
		// Anti-scraper values lifted from the site's web bundle; rotated periodically by the site.
		private const val APP_SECRET = "dfdf72051dbfdc7d76889ebd31324e74"
		private const val SECRET_SALT = "doujindesu-scrapers-cannot-read-this-super-secret-salt-2026-v2"
		private const val ENC_FIELD = "_enc_resp_"
		private const val KEY_WINDOW_MS = 3_600_000L
		private const val KEY_LENGTH = 32
	}
}

package org.koitharu.kotatsu.parsers.site.en

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.util.EnumSet
import java.util.LinkedHashSet

@MangaSourceParser("COMIX", "Comix", "en", ContentType.MANGA)
internal class Comix(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.COMIX, 28) {

	override val configKeyDomain = ConfigKey.Domain("comix.to")

	private val origin: String
		get() = "https://$domain"

	private val apiBaseUrl: String
		get() = "$origin/api/v1"

	private var secureBundle: SecureBundle? = null

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
			isMultipleTagsSupported = true,
			isTagsExclusionSupported = false,
		)

	override val availableSortOrders: Set<SortOrder> = LinkedHashSet(
		listOf(
			SortOrder.RELEVANCE,
			SortOrder.UPDATED,
			SortOrder.POPULARITY,
			SortOrder.NEWEST,
			SortOrder.ALPHABETICAL,
		),
	)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = fetchAvailableTags(),
		availableContentRating = EnumSet.of(ContentRating.ADULT),
	)

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		return setOf(
			// Genres
			MangaTag(key = "6", title = "Action", source = source),
			MangaTag(key = "7", title = "Adventure", source = source),
			MangaTag(key = "8", title = "Boys Love", source = source),
			MangaTag(key = "9", title = "Comedy", source = source),
			MangaTag(key = "10", title = "Crime", source = source),
			MangaTag(key = "11", title = "Drama", source = source),
			MangaTag(key = "12", title = "Fantasy", source = source),
			MangaTag(key = "13", title = "Girls Love", source = source),
			MangaTag(key = "14", title = "Historical", source = source),
			MangaTag(key = "15", title = "Horror", source = source),
			MangaTag(key = "16", title = "Isekai", source = source),
			MangaTag(key = "17", title = "Magical Girls", source = source),
			MangaTag(key = "87267", title = "Mature", source = source),
			MangaTag(key = "18", title = "Mecha", source = source),
			MangaTag(key = "19", title = "Medical", source = source),
			MangaTag(key = "20", title = "Mystery", source = source),
			MangaTag(key = "21", title = "Philosophical", source = source),
			MangaTag(key = "22", title = "Psychological", source = source),
			MangaTag(key = "23", title = "Romance", source = source),
			MangaTag(key = "24", title = "Sci-Fi", source = source),
			MangaTag(key = "25", title = "Slice of Life", source = source),
			MangaTag(key = "26", title = "Sports", source = source),
			MangaTag(key = "27", title = "Superhero", source = source),
			MangaTag(key = "28", title = "Thriller", source = source),
			MangaTag(key = "29", title = "Tragedy", source = source),
			MangaTag(key = "30", title = "Wuxia", source = source),
			// Themes
			MangaTag(key = "31", title = "Aliens", source = source),
			MangaTag(key = "32", title = "Animals", source = source),
			MangaTag(key = "33", title = "Cooking", source = source),
			MangaTag(key = "34", title = "Crossdressing", source = source),
			MangaTag(key = "35", title = "Delinquents", source = source),
			MangaTag(key = "36", title = "Demons", source = source),
			MangaTag(key = "37", title = "Genderswap", source = source),
			MangaTag(key = "38", title = "Ghosts", source = source),
			MangaTag(key = "39", title = "Gyaru", source = source),
			MangaTag(key = "40", title = "Harem", source = source),
			MangaTag(key = "41", title = "Incest", source = source),
			MangaTag(key = "42", title = "Loli", source = source),
			MangaTag(key = "43", title = "Mafia", source = source),
			MangaTag(key = "44", title = "Magic", source = source),
			MangaTag(key = "45", title = "Martial Arts", source = source),
			MangaTag(key = "46", title = "Military", source = source),
			MangaTag(key = "47", title = "Monster Girls", source = source),
			MangaTag(key = "48", title = "Monsters", source = source),
			MangaTag(key = "49", title = "Music", source = source),
			MangaTag(key = "50", title = "Ninja", source = source),
			MangaTag(key = "51", title = "Office Workers", source = source),
			MangaTag(key = "52", title = "Police", source = source),
			MangaTag(key = "53", title = "Post-Apocalyptic", source = source),
			MangaTag(key = "54", title = "Reincarnation", source = source),
			MangaTag(key = "55", title = "Reverse Harem", source = source),
			MangaTag(key = "56", title = "Samurai", source = source),
			MangaTag(key = "57", title = "School Life", source = source),
			MangaTag(key = "58", title = "Shota", source = source),
			MangaTag(key = "59", title = "Supernatural", source = source),
			MangaTag(key = "60", title = "Survival", source = source),
			MangaTag(key = "61", title = "Time Travel", source = source),
			MangaTag(key = "62", title = "Traditional Games", source = source),
			MangaTag(key = "63", title = "Vampires", source = source),
			MangaTag(key = "64", title = "Video Games", source = source),
			MangaTag(key = "65", title = "Villainess", source = source),
			MangaTag(key = "66", title = "Virtual Reality", source = source),
			MangaTag(key = "67", title = "Zombies", source = source),
		)
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = apiUrlBuilder("manga")
			.applyListParams(page, order, filter)
			.build()
		val result = webClient.httpGet(url, apiHeaders()).parseJson().unwrapResult()
		val items = result.getJSONArray("items")
		return List(items.length()) { i ->
			parseMangaFromJson(items.getJSONObject(i))
		}
	}

	override suspend fun getDetails(manga: Manga): Manga = coroutineScope {
		val hid = manga.comixHid()
		val chaptersDeferred = async { getChapters(manga) }
		val response = webClient.httpGet(
			apiUrlBuilder("manga", hid).build(),
			apiHeaders(manga.publicUrl),
		).parseJson()
		val result = response.unwrapResult()
		parseMangaFromJson(result).copy(
			chapters = chaptersDeferred.await(),
		)
	}

	override suspend fun getRelatedManga(seed: Manga): List<Manga> = emptyList()

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val chapterId = chapter.comixChapterId()
		val result = protectedApiGet(
			path = "chapters/$chapterId",
			url = apiUrlBuilder("chapters", chapterId),
			referer = "$origin${chapter.url}",
		)
		val pages = result.optJSONObject("pages") ?: throw ParseException("Unable to find chapter pages", chapter.url)
		val baseUrl = pages.optString("baseUrl").nullIfEmpty().orEmpty()
		val items = pages.optJSONArray("items") ?: throw ParseException("Unable to find chapter pages", chapter.url)
		return List(items.length()) { i ->
			val imageUrl = when (val item = items.get(i)) {
				is String -> item
				is JSONObject -> item.getString("url")
				else -> throw ParseException("Unexpected image format", chapter.url)
			}
			MangaPage(
				id = generateUid("$chapterId-$i"),
				url = imageUrl.withBaseUrl(baseUrl),
				preview = null,
				source = source,
			)
		}
	}

	private suspend fun getChapters(manga: Manga): List<MangaChapter> {
		val hid = manga.comixHid()
		val chapters = ArrayList<JSONObject>()
		var page = 1
		while (true) {
			val result = protectedApiGet(
				path = "manga/$hid/chapters",
				url = apiUrlBuilder("manga", hid, "chapters")
					.addQueryParameter("order[number]", "desc")
					.addQueryParameter("limit", "100")
					.addQueryParameter("page", page.toString()),
				referer = manga.publicUrl,
			)
			val items = result.optJSONArray("items") ?: JSONArray()
			for (i in 0 until items.length()) {
				chapters += items.getJSONObject(i)
			}
			val meta = result.optJSONObject("meta") ?: result.optJSONObject("pagination")
			val lastPage = meta?.optInt("lastPage", meta.optInt("last_page", page)) ?: page
			val hasNext = meta?.optBoolean("hasNext", page < lastPage) ?: false
			if (items.length() == 0 || (!hasNext && page >= lastPage)) {
				break
			}
			page++
		}

		val builder = ChaptersListBuilder(chapters.size)
		chapters.asReversed().forEach { chapter ->
			builder.add(parseChapterFromJson(chapter, manga))
		}
		return builder.toList()
	}

	private fun parseMangaFromJson(json: JSONObject): Manga {
		val hid = json.optString("hid").nullIfEmpty()
			?: json.optString("hash_id").nullIfEmpty()
			?: throw ParseException("Missing manga id", origin)
		val title = json.getString("title")
		val relativeUrl = json.optString("url").toRelativeUrl()
			?: "/title/${listOfNotNull(hid, json.optString("slug").nullIfEmpty()).joinToString("-")}"
		val publicUrl = json.optString("url").nullIfEmpty() ?: "$origin$relativeUrl"
		val poster = json.optJSONObject("poster")
		val rating = json.optDoubleOrFallback("ratedAvg", "rated_avg")

		return Manga(
			id = generateUid(hid),
			url = relativeUrl,
			publicUrl = publicUrl,
			coverUrl = poster?.optString("large")?.nullIfEmpty() ?: poster?.optString("medium")?.nullIfEmpty(),
			title = title,
			altTitles = json.parseAltTitles(),
			description = json.optString("synopsis").nullIfEmpty(),
			rating = if (rating > 0.0) (rating / 10.0).toFloat() else RATING_UNKNOWN,
			tags = json.parseTags(),
			authors = json.parsePeople("authors", "artists"),
			state = json.optString("status").toMangaState(),
			source = source,
			contentRating = json.toContentRating(),
		)
	}

	private fun parseChapterFromJson(json: JSONObject, manga: Manga): MangaChapter {
		val chapterId = json.optLong("id", json.optLong("chapter_id"))
		if (chapterId == 0L) {
			throw ParseException("Missing chapter id", manga.publicUrl)
		}
		val number = json.optDouble("number", 0.0).toFloat()
		val group = json.optJSONObject("group") ?: json.optJSONObject("scanlation_group")
		val groupName = group?.optString("name")?.nullIfEmpty()
		val url = json.optString("url").toRelativeUrl()
			?: "${manga.url}/$chapterId-chapter-${number.toChapterUrlPart()}"
		return MangaChapter(
			id = generateUid("${groupName.orEmpty()}-$chapterId"),
			title = json.optString("name").nullIfEmpty(),
			number = number,
			volume = json.optInt("volume", 0),
			url = url,
			uploadDate = json.parseUploadDate(),
			source = source,
			scanlator = groupName,
			branch = groupName,
		)
	}

	private suspend fun protectedApiGet(
		path: String,
		url: HttpUrl.Builder,
		referer: String,
	): JSONObject {
		val token = signApiPath(path)
		val payload = webClient.httpGet(
			url.addQueryParameter("_", token).build(),
			apiHeaders(referer),
		).parseJson()
		return decryptApiPayload(path, payload)
	}

	private suspend fun signApiPath(path: String): String {
		val script = secureScript(
			"""
				/* __COMIX_SIGN__ */
				var requestInterceptors = [];
				var fakeAxios = {
					interceptors: {
						request: { use: function(fn) { requestInterceptors.push(fn); } },
						response: { use: function() {} }
					},
					defaults: { headers: { common: {} } }
				};
				v(fakeAxios);
				var request = {
					method: "get",
					baseURL: "/api/v1",
					url: ${JSONObject.quote("/$path")},
					params: {},
					headers: {}
				};
				for (var i = 0; i < requestInterceptors.length; i++) {
					var next = requestInterceptors[i](request);
					if (next) request = next;
				}
				request.params._;
			""".trimIndent(),
		)
		return decodeJsString(context.evaluateJs(origin, script, SECURE_EVAL_TIMEOUT))
	}

	private suspend fun decryptApiPayload(path: String, payload: JSONObject): JSONObject {
		if (!payload.has("e")) {
			return payload.unwrapResult()
		}
		val script = secureScript(
			"""
				/* __COMIX_DECRYPT__ */
				var responseInterceptors = [];
				var fakeAxios = {
					interceptors: {
						request: { use: function() {} },
						response: { use: function(fn) { responseInterceptors.push(fn); } }
					},
					defaults: { headers: { common: {} } }
				};
				v(fakeAxios);
				var response = {
					data: JSON.parse(${JSONObject.quote(payload.toString())}),
					headers: { "x-enc": "1", "content-type": "application/json" },
					config: {
						method: "get",
						baseURL: "/api/v1",
						url: ${JSONObject.quote("/$path")},
						params: {},
						headers: {}
					}
				};
				for (var i = 0; i < responseInterceptors.length; i++) {
					var next = responseInterceptors[i](response);
					if (next) response = next;
				}
				JSON.stringify(response.data);
			""".trimIndent(),
		)
		val raw = decodeJsString(context.evaluateJs(origin, script, SECURE_EVAL_TIMEOUT))
		return JSONObject(raw)
	}

	private suspend fun secureScript(body: String): String {
		val bundle = loadSecureBundle()
		return """
			(function() {
				var meta = document.querySelector('meta[name="cfg"]');
				if (!meta) {
					meta = document.createElement("meta");
					meta.setAttribute("name", "cfg");
					document.head.appendChild(meta);
				}
				meta.setAttribute("content", ${JSONObject.quote(bundle.cfg)});
				${bundle.script}
				$body
			})()
		""".trimIndent()
	}

	private suspend fun loadSecureBundle(): SecureBundle {
		secureBundle?.let { return it }
		val home = webClient.httpGet("$origin/home", apiHeaders()).parseHtml()
		val cfg = home.selectFirst("meta[name=cfg]")?.attr("content")?.nullIfEmpty()
			?: throw ParseException("Missing Comix secure config", "$origin/home")
		val mainUrl = home.select("script[src]")
			.asSequence()
			.mapNotNull { it.absUrl("src").nullIfEmpty() }
			.firstOrNull { it.contains("/main-") || it.endsWith("/main.js") }
			?: throw ParseException("Missing Comix main bundle", "$origin/home")
		val mainScript = webClient.httpGet(mainUrl, apiHeaders()).parseRaw()
		val secureRef = Regex("""from\s*["'](\./secure[^"']+\.js)["']""")
			.find(mainScript)
			?.groupValues
			?.get(1)
			?: throw ParseException("Missing Comix secure bundle", mainUrl)
		val secureUrl = mainUrl.toHttpUrl().resolve(secureRef)
			?: throw ParseException("Invalid Comix secure bundle url", mainUrl)
		val secureScript = webClient.httpGet(secureUrl, apiHeaders()).parseRaw()
			.replace(Regex("""export\s*\{[^}]+}\s*;?\s*$"""), "")
		return SecureBundle(cfg = cfg, script = secureScript).also {
			secureBundle = it
		}
	}

	private fun apiUrlBuilder(vararg pathSegments: String): HttpUrl.Builder {
		return apiBaseUrl.toHttpUrl().newBuilder().apply {
			pathSegments.forEach(::addPathSegment)
		}
	}

	private fun HttpUrl.Builder.applyListParams(
		page: Int,
		order: SortOrder,
		filter: MangaListFilter,
	): HttpUrl.Builder = apply {
		filter.query?.takeUnless(String::isBlank)?.let {
			addQueryParameter("keyword", it)
		}
		when (order) {
			SortOrder.RELEVANCE -> addQueryParameter("order[relevance]", "desc")
			SortOrder.UPDATED -> addQueryParameter("order[chapter_updated_at]", "desc")
			SortOrder.POPULARITY -> addQueryParameter("order[views_30d]", "desc")
			SortOrder.NEWEST -> addQueryParameter("order[created_at]", "desc")
			SortOrder.ALPHABETICAL -> addQueryParameter("order[title]", "asc")
			else -> addQueryParameter("order[chapter_updated_at]", "desc")
		}
		for (tag in filter.tags) {
			addQueryParameter("genres_in[]", tag.key)
		}
		addQueryParameter(
			"content_rating",
			when {
				ContentRating.ADULT in filter.contentRating -> "pornographic"
				ContentRating.SUGGESTIVE in filter.contentRating -> "suggestive"
				ContentRating.SAFE in filter.contentRating -> "safe"
				else -> "suggestive"
			},
		)
		addQueryParameter("limit", pageSize.toString())
		addQueryParameter("page", page.toString())
	}

	private fun apiHeaders(referer: String = "$origin/"): Headers {
		return getRequestHeaders().newBuilder()
			.set("Accept", "application/json")
			.set("X-Requested-With", "XMLHttpRequest")
			.set("Origin", origin)
			.set("Referer", referer)
			.build()
	}

	private fun JSONObject.unwrapResult(): JSONObject = optJSONObject("result") ?: this

	private fun JSONObject.optDoubleOrFallback(primary: String, fallback: String): Double {
		return if (has(primary)) optDouble(primary, 0.0) else optDouble(fallback, 0.0)
	}

	private fun JSONObject.parseAltTitles(): Set<String> {
		val array = optJSONArray("altTitles") ?: optJSONArray("alt_titles") ?: return emptySet()
		val result = LinkedHashSet<String>(array.length())
		for (i in 0 until array.length()) {
			when (val item = array.get(i)) {
				is String -> item.nullIfEmpty()?.let(result::add)
				is JSONObject -> (item.optString("title").nullIfEmpty() ?: item.optString("name").nullIfEmpty())?.let(result::add)
			}
		}
		return result
	}

	private fun JSONObject.parseTags(): Set<MangaTag> {
		val result = LinkedHashSet<MangaTag>()
		for (key in TAG_ARRAY_KEYS) {
			val array = optJSONArray(key) ?: continue
			for (i in 0 until array.length()) {
				val item = array.optJSONObject(i) ?: continue
				val title = item.optString("name").nullIfEmpty()
					?: item.optString("title").nullIfEmpty()
					?: continue
				val tagKey = item.optString("id").nullIfEmpty()
					?: item.optString("slug").nullIfEmpty()
					?: title
				result += MangaTag(key = tagKey, title = title, source = source)
			}
		}
		return result
	}

	private fun JSONObject.parsePeople(vararg keys: String): Set<String> {
		val result = LinkedHashSet<String>()
		for (key in keys) {
			val array = optJSONArray(key) ?: continue
			for (i in 0 until array.length()) {
				when (val item = array.get(i)) {
					is String -> item.nullIfEmpty()?.let(result::add)
					is JSONObject -> item.optString("name").nullIfEmpty()?.let(result::add)
				}
			}
		}
		return result
	}

	private fun JSONObject.toContentRating(): ContentRating {
		return when (optString("contentRating").lowercase()) {
			"safe" -> ContentRating.SAFE
			"suggestive" -> ContentRating.SUGGESTIVE
			"erotica", "pornographic" -> ContentRating.ADULT
			else -> if (optBoolean("is_nsfw", false)) ContentRating.ADULT else ContentRating.SAFE
		}
	}

	private fun JSONObject.parseUploadDate(): Long {
		val timestamp = optLong("createdAt", optLong("created_at", 0L))
		return when {
			timestamp <= 0L -> 0L
			timestamp > 10_000_000_000L -> timestamp
			else -> timestamp * 1000L
		}
	}

	private fun String.toMangaState(): MangaState? = when (lowercase()) {
		"finished", "completed", "complete" -> MangaState.FINISHED
		"releasing", "ongoing" -> MangaState.ONGOING
		"on_hiatus", "hiatus" -> MangaState.PAUSED
		"cancelled", "canceled", "dropped" -> MangaState.ABANDONED
		else -> null
	}

	private fun String?.toRelativeUrl(): String? {
		val value = this?.nullIfEmpty() ?: return null
		if (value.startsWith('/')) {
			return value
		}
		val schemePos = value.indexOf("://")
		if (schemePos == -1) {
			return null
		}
		val pathStart = value.indexOf('/', startIndex = schemePos + 3)
		return if (pathStart == -1) null else value.substring(pathStart)
	}

	private fun String.withBaseUrl(baseUrl: String): String {
		return if (startsWith("http://") || startsWith("https://")) {
			this
		} else {
			baseUrl + this
		}
	}

	private fun Manga.comixHid(): String {
		return url.substringAfter("/title/").substringBefore('/').substringBefore('-')
	}

	private fun MangaChapter.comixChapterId(): String {
		return url.substringAfterLast('/').substringBefore('-')
	}

	private fun Float.toChapterUrlPart(): String {
		val intValue = toInt()
		return if (this == intValue.toFloat()) intValue.toString() else toString()
	}

	private fun decodeJsString(raw: String?): String {
		val value = raw ?: throw ParseException("Comix secure script returned null", origin)
		return if (value.startsWith('"')) {
			JSONArray("[$value]").getString(0)
		} else {
			value
		}
	}

	private data class SecureBundle(
		val cfg: String,
		val script: String,
	)

	private companion object {
		private const val SECURE_EVAL_TIMEOUT = 10_000L
		private val TAG_ARRAY_KEYS = arrayOf("genres", "demographics", "formats", "tags")
	}
}

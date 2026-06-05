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
import org.koitharu.kotatsu.parsers.webview.InterceptionConfig
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.EnumSet
import java.util.LinkedHashSet
import java.util.Locale

@MangaSourceParser("COMIX", "Comix", "en", ContentType.MANGA)
internal class Comix(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.COMIX, 28) {

	override val configKeyDomain = ConfigKey.Domain("comix.to")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	private val origin: String
		get() = "https://$domain"

	private val apiBaseUrl: String
		get() = "$origin/api/v1"

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

	private fun fetchAvailableTags(): Set<MangaTag> {
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
		val response = webViewApiJson("/api/v1/chapters/$chapterId")
		val result = response.unwrapResult()
		val pages = result.optJSONObject("pages") ?: throw ParseException("Unable to find chapter pages", chapter.url)
		val baseUrl = pages.optString("baseUrl").nullIfEmpty().orEmpty().trimEnd('/')
		val items = pages.optJSONArray("items") ?: throw ParseException("Unable to find chapter pages", chapter.url)
		return List(items.length()) { i ->
			val (rawUrl, scrambled) = when (val item = items.get(i)) {
				is String -> item to false
				is JSONObject -> item.getString("url") to (item.optInt("s", 0) != 0)
				else -> throw ParseException("Unexpected image format", chapter.url)
			}
			// Pages flagged scrambled (s == 1) are served pre-shuffled at `/i[3]/…`. Comix's CDN
			// exposes a parallel `/si/…` endpoint that returns the same image already descrambled
			// server-side; rewriting the path is the entire fix. Their frontend already does this
			// for the legacy `/i/` paths; we extend it to the newer `/i3/` ones too.
			val finalUrl = rawUrl.withBaseUrl(baseUrl).let { url ->
				if (scrambled) url.replace(SCRAMBLE_URL_REGEX, "/si/") else url
			}
			MangaPage(
				id = generateUid("$chapterId-$i"),
				url = finalUrl,
				preview = null,
				source = source,
			)
		}
	}

	private suspend fun getChapters(manga: Manga): List<MangaChapter> {
		val hid = manga.comixHid()
		val payload = webViewChapterList(hid)
		val items = payload.optJSONArray("items") ?: JSONArray()
		val builder = ChaptersListBuilder(items.length())
		for (i in 0 until items.length()) {
			builder.add(parseChapterFromJson(items.getJSONObject(i), manga))
		}
		return builder.toList().reversed()
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
		val scanlator = groupName ?: if (json.optBoolean("isOfficial")) "Official" else null
		val url = json.optString("url").toRelativeUrl()
			?: "${manga.url}/$chapterId-chapter-${number.toChapterUrlPart()}"
		return MangaChapter(
			id = generateUid("${scanlator.orEmpty()}-$chapterId"),
			title = json.optString("name").nullIfEmpty(),
			number = number,
			volume = json.optInt("volume", 0),
			url = url,
			uploadDate = json.parseUploadDate(),
			source = source,
			scanlator = scanlator,
			branch = scanlator,
		)
	}

	/**
	 * Calls a protected `/api/v1/...` endpoint via a WebView bridge.
	 *
	 * Comix obfuscates two functions inside a `vmX_yyyy` namespace on `window`: a request
	 * signer (computes the `_` query token) and an axios response interceptor (decrypts the
	 * `e` payload). The legacy "rip out the secure bundle and run it in Rhino" approach
	 * stopped working when Comix tightened the bundle; this WebView bridge mirrors the
	 * approach upstream Kotatsu landed.
	 */
	private suspend fun webViewApiJson(apiPath: String): JSONObject {
		return evaluateWebViewJson(
			label = apiPath,
			script = buildWebViewApiScript("return JSON.stringify(await fetchProtected(${apiPath.toJsString()}));"),
		)
	}

	private suspend fun webViewChapterList(hid: String): JSONObject {
		val pathPrefix = "/api/v1/manga/$hid/chapters?page="
		return evaluateWebViewJson(
			label = "chapters:$hid",
			script = buildWebViewApiScript(
				"""
					const all = [];
					const compact = (item) => ({
						id: item.id,
						number: item.number,
						name: item.name || "",
						volume: item.volume || 0,
						createdAt: item.createdAt || item.created_at || 0,
						createdAtFormatted: item.createdAtFormatted || "",
						isOfficial: !!item.isOfficial,
						group: item.group || item.scanlation_group || null
					});
					const pagePath = (page) =>
						${pathPrefix.toJsString()} + page +
							"&limit=$CHAPTER_API_LIMIT&order%5Bnumber%5D=desc";
					const pageInfo = (result, fallbackPage) => {
						const pagination = (result && (result.pagination || result.meta)) || {};
						return {
							current: Number(pagination.page || pagination.current_page || fallbackPage),
							last: Number(pagination.lastPage || pagination.last_page || 1)
						};
					};
					let page = 1;
					while (page <= $MAX_CHAPTER_API_PAGES) {
						const root = await fetchProtected(pagePath(page));
						const result = root && root.result ? root.result : root;
						if (!result || !Array.isArray(result.items)) {
							const keys = result && typeof result === "object" ? Object.keys(result).join(",") : typeof result;
							throw new Error("chapter payload has no items; keys=" + keys);
						}
						const items = result.items;
						for (const item of items) all.push(compact(item));
						const pagination = pageInfo(result, page);
						if (!items.length || pagination.current >= pagination.last) break;
						page++;
					}
					return JSON.stringify({ items: all });
				""".trimIndent(),
			),
		)
	}

	private suspend fun evaluateWebViewJson(label: String, script: String): JSONObject {
		val startedAt = System.currentTimeMillis()
		val bridgeUrl = "$origin/?kotatsu_comix_bridge=$startedAt"
		val requests = runCatching {
			context.interceptWebViewRequests(
				bridgeUrl,
				InterceptionConfig(
					timeoutMs = WEBVIEW_API_TIMEOUT,
					maxRequests = 1,
					urlPattern = INTERCEPT_URL_REGEX,
					pageScript = script,
				),
			)
		}.getOrElse { e ->
			throw ParseException("Comix WebView bridge failed for $label", bridgeUrl, e)
		}
		val resultUrl = requests.firstOrNull()?.url
			?: throw ParseException("Comix WebView bridge produced no result for $label", bridgeUrl)
		if (resultUrl.contains("/error", ignoreCase = true)) {
			val message = resultUrl.queryParameterValue("msg") ?: "unknown WebView error"
			throw ParseException("Comix WebView bridge failed for $label: $message", bridgeUrl)
		}
		val decoded = resultUrl.queryParameterValue("data")
			?: throw ParseException("Comix WebView bridge result missing data for $label", bridgeUrl)
		if (decoded == CLOUDFLARE_BLOCKED || isCloudflarePage(decoded)) {
			requestCloudflareVerification(bridgeUrl)
		}
		if (decoded.isBlank()) {
			throw ParseException("Comix WebView bridge returned empty payload for $label", bridgeUrl)
		}
		val json = runCatching { JSONObject(decoded) }.getOrElse { e ->
			throw ParseException("Comix WebView bridge returned invalid JSON: ${decoded.take(200)}", bridgeUrl, e)
		}
		json.optString("error").nullIfEmpty()?.let { error ->
			throw ParseException("Comix WebView bridge failed for $label: $error", bridgeUrl)
		}
		return json
	}

	private fun buildWebViewApiScript(body: String): String {
		// The pageScript is wrapped by the host so it receives a `return value` from the inner closure.
		// We emit a self-invoking async closure that posts result/error to the intercepted bridge URL.
		val inner = """
			(async () => {
				const probePath = "/manga/g2rk/chapters";
				const tokenRegex = /^[A-Za-z0-9_-]{20,200}$/;
				const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));
				const challengeDetected = () => {
					const root = document.documentElement;
					const html = (root && root.outerHTML) || "";
					const text = ((document.body && document.body.innerText) || (root && root.innerText) || "");
					const lower = (document.title + "\n" + text + "\n" + html).toLowerCase();
					return document.querySelector('script[src*="challenge-platform"]') !== null ||
						document.querySelector('script[src*="turnstile"]') !== null ||
						document.querySelector('iframe[src*="challenges.cloudflare.com"]') !== null ||
						document.querySelector('.cf-turnstile') !== null ||
						document.querySelector('form[action*="__cf_chl"]') !== null ||
						document.querySelector('.cf-browser-verification') !== null ||
						((lower.includes('just a moment') || lower.includes('checking your browser')) && lower.includes('cloudflare')) ||
						lower.includes('challenge-platform') ||
						lower.includes('challenges.cloudflare.com') ||
						lower.includes('cf-turnstile') ||
						lower.includes('turnstile') ||
						lower.includes('cf-chl-opt');
				};
				const findGlue = () => {
					let signer = null;
					let installer = null;
					let responseHandler = null;
					const keys = Object.keys(window);
					for (let i = 0; i < keys.length; i++) {
						const topName = keys[i];
						if (!/^vm[A-Za-z]_\w+${'$'}/.test(topName)) continue;
						const ns = window[topName];
						if (!ns || typeof ns !== "object") continue;
						const fnames = Object.keys(ns);
						for (let j = 0; j < fnames.length; j++) {
							const fn = ns[fnames[j]];
							if (typeof fn !== "function") continue;
							if (!signer) {
								try {
									const out = fn(probePath);
									if (typeof out === "string" && out !== probePath && tokenRegex.test(out)) {
										signer = fn;
									}
								} catch (e) {}
							}
							if (!installer) {
								try {
									let got = false;
									let resFn = null;
									const fakeAxios = {
										interceptors: {
											request: { use: function() {} },
											response: { use: function(fn) { got = true; resFn = fn; } }
										},
										defaults: { headers: { common: {} }, transformRequest: [], transformResponse: [] }
									};
									fn(fakeAxios);
									if (got) {
										installer = fn;
										responseHandler = resFn;
									}
								} catch (e) {}
							}
							if (signer && installer) return { signer, installer, responseHandler };
						}
					}
					return null;
				};

				try {
					let glue = null;
					for (let attempt = 0; attempt < 80; attempt++) {
						if (challengeDetected()) {
							return "$CLOUDFLARE_BLOCKED";
						}
						glue = findGlue();
						if (glue) break;
						await sleep(250);
					}
					if (!glue) throw new Error("signer/decryptor not detected");

					const captured = { res: glue.responseHandler || null };
					if (!captured.res) {
						const fakeAxios = {
							interceptors: {
								request: { use: function() {} },
								response: { use: function(fn) { captured.res = fn; } }
							},
							defaults: { headers: { common: {} }, transformRequest: [], transformResponse: [] }
						};
						glue.installer(fakeAxios);
					}

					const signCandidates = (apiPath) => {
						const withoutApi = apiPath.replace(/^\/api\/v1/, "");
						const withoutQuery = withoutApi.split("?")[0];
						const decoded = (() => {
							try { return decodeURIComponent(withoutApi); } catch (e) { return withoutApi; }
						})();
						return [...new Set([withoutApi, decoded, withoutQuery])];
					};

					const fetchProtected = async (apiPath) => {
						const sep = apiPath.indexOf("?") === -1 ? "?" : "&";
						let resp = null;
						let text = "";
						let signedUrl = "";
						let lastError = "";
						const candidates = signCandidates(apiPath);
						for (const signablePath of candidates) {
							const sig = glue.signer(signablePath);
							if (!sig) {
								lastError = "signer returned empty token";
								continue;
							}
							signedUrl = apiPath + sep + "_=" + encodeURIComponent(sig);
							resp = await fetch(signedUrl, {
								credentials: "include",
								headers: { "Accept": "application/json", "X-Requested-With": "XMLHttpRequest" }
							});
							text = await resp.text();
							if (resp.status >= 200 && resp.status < 300) break;
							lastError = "HTTP " + resp.status + " signed=" + signablePath + ": " + text.slice(0, 200);
							if (resp.status !== 422) break;
						}
						if (!resp) throw new Error(lastError || "request was not sent");
						if (resp.status < 200 || resp.status >= 300) {
							throw new Error(lastError || ("HTTP " + resp.status + ": " + text.slice(0, 200)));
						}
						const raw = JSON.parse(text);
						if (raw && typeof raw === "object" && "e" in raw && captured.res) {
							const fakeResp = {
								data: raw,
								status: resp.status,
								statusText: resp.statusText,
								headers: Object.fromEntries([...resp.headers.entries()]),
								config: { url: signedUrl, method: "get", baseURL: "/api/v1" },
								request: {}
							};
							const decoded = await captured.res(fakeResp);
							return { result: decoded && decoded.data };
						}
						if (raw && typeof raw === "object" && "e" in raw) {
							throw new Error("encrypted response received but decryptor was not captured");
						}
						if (raw && typeof raw === "object" && "result" in raw) return raw;
						return { result: raw };
					};

					$body
				} catch (e) {
					return JSON.stringify({ error: String((e && e.message) || e) });
				}
			})()
		""".trimIndent()
		return """
			(async function() {
				try {
					const result = await $inner;
					window.location.href = "$INTERCEPT_RESULT_URL#data=" + encodeURIComponent(String(result == null ? "" : result));
				} catch (e) {
					window.location.href = "$INTERCEPT_ERROR_URL#msg=" + encodeURIComponent(String((e && e.message) || e));
				}
			})();
		""".trimIndent()
	}

	private fun requestCloudflareVerification(url: String, cause: Throwable? = null): Nothing {
		try {
			context.requestBrowserAction(this, url)
		} catch (e: UnsupportedOperationException) {
			throw ParseException(CLOUDFLARE_MESSAGE, url, cause ?: e)
		}
	}

	private fun isCloudflarePage(html: String): Boolean {
		if (html.isBlank()) return false
		val lower = html.lowercase(Locale.US)
		return lower.contains("<title>just a moment") ||
			((lower.contains("just a moment") || lower.contains("checking your browser")) && lower.contains("cloudflare")) ||
			lower.contains("cf-browser-verification") ||
			lower.contains("cf-chl-opt") ||
			lower.contains("challenge-platform") ||
			lower.contains("challenges.cloudflare.com") ||
			lower.contains("cf-turnstile") ||
			lower.contains("turnstile")
	}

	private fun String.queryParameterValue(name: String): String? {
		val query = substringAfter('#', substringAfter('?', ""))
		if (query.isEmpty()) return null
		return query.split('&')
			.asSequence()
			.map { it.split('=', limit = 2) }
			.firstOrNull { it.size == 2 && it[0] == name }
			?.get(1)
			?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
	}

	private fun String.toJsString(): String {
		return "\"" + replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t") + "\""
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
		"cancelled", "canceled", "dropped", "discontinued" -> MangaState.ABANDONED
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
			"$baseUrl/${trimStart('/')}"
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

	private companion object {
		private const val WEBVIEW_API_TIMEOUT = 90_000L
		private const val CHAPTER_API_LIMIT = 100
		private const val MAX_CHAPTER_API_PAGES = 30
		private const val CLOUDFLARE_BLOCKED = "CLOUDFLARE_BLOCKED"
		private const val INTERCEPT_RESULT_URL = "https://kotatsu.intercept/result"
		private const val INTERCEPT_ERROR_URL = "https://kotatsu.intercept/error"
		private val INTERCEPT_URL_REGEX = Regex("https://kotatsu\\.intercept/.*", RegexOption.IGNORE_CASE)
		private const val CLOUDFLARE_MESSAGE = "Cloudflare verification is required. Open Comix in the in-app browser, complete the check, then try again."
		private val TAG_ARRAY_KEYS = arrayOf("genres", "demographics", "formats", "tags")
		// Matches Comix's image-CDN scrambled-tier path prefix. Their frontend rewrites only
		// `/i/[bh]…` paths to `/si/…`; the newer `/i3/…` tier needs the same swap.
		private val SCRAMBLE_URL_REGEX = Regex("""/i3?/""")
	}
}

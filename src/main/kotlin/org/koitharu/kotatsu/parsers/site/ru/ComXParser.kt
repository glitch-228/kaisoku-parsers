package org.koitharu.kotatsu.parsers.site.en

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaParserAuthProvider
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.AuthRequiredException
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.network.UserAgents
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.getFloatOrDefault
import org.koitharu.kotatsu.parsers.util.json.getIntOrDefault
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.suspendlazy.getOrNull
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("COMX", "Com-X", "ru", ContentType.COMICS)
internal class ComXParser(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.COMX, 20),
	MangaParserAuthProvider {

	override val configKeyDomain = ConfigKey.Domain("com-x.life", "comx.life")

	override val userAgentKey = ConfigKey.UserAgent(UserAgents.CHROME_MOBILE)

	private val availableTags = suspendLazy(initializer = ::fetchTags)
	private val cdnImageUrl = "img.com-x.life/comix/"
	private val noRedirectClient by lazy {
		context.httpClient.newBuilder()
			.followRedirects(false)
			.followSslRedirects(false)
			.build()
	}

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	init {
		context.cookieJar.insertCookies(domain, "adt-accepted", "1")
	}

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED)

	override val authUrl: String
		get() = "https://$domain/"

	override suspend fun isAuthorized(): Boolean {
		val cookies = context.cookieJar.getCookies(domain)
		return cookies.any { it.name == AUTH_COOKIE_ID } && cookies.any { it.name == AUTH_COOKIE_PASSWORD }
	}

	override suspend fun getUsername(): String {
		val userId = context.cookieJar.getCookies(domain).firstOrNull { it.name == AUTH_COOKIE_ID }?.value
			?.takeUnless { it == "0" }
			?: throw AuthRequiredException(source)
		val doc = webClient.httpGetPublic("https://$domain/")
		if (doc.isAuthPage()) {
			throw AuthRequiredException(source)
		}
		return doc.selectFirst("a[href*=/user/], a[href*=\"/index.php?subaction=userinfo\"]")
			?.textOrNull()
			?.nullIfEmpty()
			?: "User $userId"
	}

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isMultipleTagsSupported = true,
			isYearRangeSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = availableTags.get(),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val urlBuilder = StringBuilder()
		when {
			!filter.query.isNullOrEmpty() -> {
				val encodedQuery = filter.query.splitByWhitespace().joinToString(separator = "%20") { part ->
					part.urlEncoded()
				}
				urlBuilder.append("/search/")
				urlBuilder.append(encodedQuery)
				if (page > 1) {
					urlBuilder.append("/page/$page/")
				}
			}

			else -> {
				urlBuilder.append("/ComicList")
				if (filter.yearFrom != YEAR_UNKNOWN) {
					urlBuilder.append("/y[from]=${filter.yearFrom}")
				}
				if (filter.yearTo != YEAR_UNKNOWN) {
					urlBuilder.append("/y[to]=${filter.yearTo}")
				}
				if (filter.tags.isNotEmpty()) {
					urlBuilder.append("/g=")
					urlBuilder.append(filter.tags.joinToString(",") { it.key })
				}
				urlBuilder.append("/sort")
				if (page > 1) {
					urlBuilder.append("/page/$page/")
				}
			}
		}

		val fullUrl = urlBuilder.toString().toAbsoluteUrl(domain)
		val doc = webClient.httpGetPublic(fullUrl)
		return doc.select("div.readed.d-flex.short").map { item ->
			val a = item.selectFirstOrThrow("a.readed__img.img-fit-cover.anim")
			val img = item.selectFirst("img[data-src]")
			val href = a.attrAsRelativeUrl("href")
			val titleElement = item.selectFirstOrThrow("h3.readed__title a")
			val (mainTitle, altTitle) = titleElement.text()
				.split("\\s*/\\s*".toRegex())
				.map { it.trim() }
				.let { parts ->
					when {
						parts.size >= 2 -> parts[1] to parts[0]
						parts.isNotEmpty() -> parts[0] to ""
						else -> "" to ""
					}
				}

			Manga(
				id = generateUid(href),
				url = href,
				publicUrl = a.attrAsAbsoluteUrl("href"),
				title = mainTitle,
				altTitles = if (altTitle.isNotEmpty()) setOf(altTitle) else emptySet(),
				authors = emptySet(),
				description = null,
				tags = emptySet(),
				rating = RATING_UNKNOWN,
				state = null,
				coverUrl = img?.attrAsAbsoluteUrlOrNull("data-src"),
				contentRating = if (isNsfwSource) ContentRating.ADULT else null,
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGetPublic(manga.url.toAbsoluteUrl(domain))

		val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.US)

		val scriptData = doc.selectFirst("script:containsData(__DATA__)")?.data()
			?.substringAfter("window.__DATA__ = ")
			?.substringBefore(";</script>")
			?.trim()
			?: throw ParseException("Script data not found", manga.url)

		val jsonData = JSONObject(scriptData)
		val chaptersJson = jsonData.getJSONArray("chapters")
		val newsId = jsonData.getLong("news_id")

		val chapters = List(chaptersJson.length()) { i ->
			val chapter = chaptersJson.getJSONObject(i)
			val chapterId = chapter.getLong("id")

			MangaChapter(
				id = generateUid("$newsId/$chapterId"),
				url = "/reader/$newsId/$chapterId",
				number = chapter.getFloatOrDefault("posi", 0f),
				title = decodeText(chapter.getStringOrNull("title")),
				uploadDate = dateFormat.parseSafe(chapter.getStringOrNull("date")),
				source = source,
				scanlator = null,
				branch = null,
				volume = chapter.getIntOrDefault("volume", 0),
			)
		}.reversed()

		val author = doc.selectFirst("li:contains(Publisher:)")
			?.textOrNull()
			?.substringAfter("Publisher:")
			?.trim()
			?.nullIfEmpty()
		val state = when (
			doc.selectFirst("li:contains(Release type:)")?.text()?.substringAfter("Release type:")?.trim()
		) {
			"Ongoing" -> MangaState.ONGOING
			else -> MangaState.FINISHED
		}

		val tagLinks = doc.getElementsByAttributeValueContaining("href", "/genres/")
		val tags = if (tagLinks.isNotEmpty()) {
			availableTags.getOrNull()?.let { allTags ->
				tagLinks.mapNotNullToSet { a ->
					val tagName = a.text()
					allTags.find { it.title.equals(tagName, ignoreCase = true) }
				}
			}
		} else {
			null
		}

		return manga.copy(
			authors = setOfNotNull(author),
			state = state,
			chapters = chapters,
			description = doc.select("div.page__text.full-text.clearfix").textOrNull(),
			tags = tags ?: manga.tags,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val newsId = chapter.url.substringAfter("/reader/").substringBefore("/")
		context.cookieJar.insertCookies(domain, "adult=$newsId")

		val doc = webClient.httpGetPublic(chapter.url.toAbsoluteUrl(domain))
		val data = doc.selectFirst("script:containsData(__DATA__)")?.data()
			?.substringAfter("=")
			?.trim()
			?.removeSuffix(";")
			?.substringAfter("\"images\":[")
			?.substringBefore("]")
			?.split(",")
			?.map { it.trim().removeSurrounding("\"").replace("\\", "") }
			?: throw ParseException("Image data not found", chapter.url)

		return data.map { imageUrl ->
			val finalUrl = "https://$cdnImageUrl$imageUrl"
			MangaPage(
				id = generateUid(imageUrl),
				url = finalUrl,
				preview = null,
				source = source,
			)
		}
	}

	private suspend fun fetchTags(): Set<MangaTag> {
		val doc = webClient.httpGetPublic("https://$domain/comix/")
		val scriptData = doc.selectFirstOrThrow("script:containsData(__XFILTER__)").data()

		val genresJson = scriptData
			.substringAfter("\"g\":{")
			.substringBefore("}}}") + "}"

		val genresObj = JSONObject("{$genresJson}")
		val valuesArray = genresObj.getJSONArray("values")

		return Set(valuesArray.length()) { i ->
			val genre = valuesArray.getJSONObject(i)
			MangaTag(
				key = genre.getInt("id").toString(),
				title = genre.getString("value").toTitleCase(sourceLocale),
				source = source,
			)
		}
	}

	private fun decodeText(text: String?): String? {
		if (text == null) return null
		return try {
			text.replace("\\u([0-9a-fA-F]{4})".toRegex()) { matchResult ->
				val codePoint = matchResult.groupValues[1].toInt(16)
				codePoint.toChar().toString()
			}
		} catch (e: Exception) {
			text
		}
	}

	private suspend fun org.koitharu.kotatsu.parsers.network.WebClient.httpGetPublic(
		url: String,
		guardAttempts: Int = 1,
	): Document {
		val response = noRedirectClient.newCall(
			Request.Builder()
				.get()
				.url(url)
				.headers(getRequestHeaders())
				.tag(MangaSource::class.java, source)
				.build(),
		).await()
		if (response.isGuardRedirect()) {
			val location = response.header("Location") ?: throw ParseException("Guard URL not found", url)
			response.close()
			if (guardAttempts == 0) {
				throw ParseException("Guard challenge loop", url)
			}
			solveGuardChallenge(location)
			return httpGetPublic(url, guardAttempts - 1)
		}
		if (response.code == 401) {
			response.close()
			throw AuthRequiredException(source)
		}
		if (response.code in 400..599) {
			val exception = HttpStatusException(response.message, response.code, url)
			response.close()
			throw exception
		}
		return response.parseHtml().also { doc ->
			if (doc.isAuthPage()) {
				throw AuthRequiredException(source)
			}
		}
	}

	private suspend fun solveGuardChallenge(location: String) {
		val challengeUrl = location.toAbsoluteUrl(domain).toHttpUrl()
		val token = challengeUrl.queryParameter("t") ?: throw ParseException("Guard token not found", location)
		val body = FormBody.Builder()
			.add("token", token)
			.add("mode", "legacy")
			.add("workTime", "553")
			.add("iterations", "108000")
			.add("webdriver", "0")
			.add("touch", "1")
			.add("screen_w", "393")
			.add("screen_h", "873")
			.add("screen_cd", "24")
			.add("tz", "-180")
			.add("dpr", "2.75")
			.add("cdp", "0")
			.add("cdpf", "")
			.build()
		val response = noRedirectClient.newCall(
			Request.Builder()
				.post(body)
				.url("https://$domain/_v")
				.headers(getRequestHeaders())
				.header("Origin", "https://$domain")
				.header("Referer", challengeUrl.toString())
				.tag(MangaSource::class.java, source)
				.build(),
		).await()
		if (response.code in 400..599) {
			val exception = HttpStatusException(response.message, response.code, response.request.url.toString())
			response.close()
			throw exception
		}
		response.close()
	}

	private fun okhttp3.Response.isGuardRedirect(): Boolean {
		if (code !in 300..399) {
			return false
		}
		val location = header("Location") ?: return false
		return location.toAbsoluteUrl(domain).toHttpUrl().encodedPath == "/_c"
	}

	private fun Document.isAuthPage(): Boolean {
		return title().contains("401", ignoreCase = true) ||
			text().contains("Требуется вход", ignoreCase = true)
	}

	private companion object {
		private const val AUTH_COOKIE_ID = "dle_user_id"
		private const val AUTH_COOKIE_PASSWORD = "dle_password"
	}
}

package org.koitharu.kotatsu.parsers.site.id

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*

/** HTML contract adapted from UMA's CGBUM adapter, with source-owned filter values. */
@MangaSourceParser("CGBUM", "CGBUM", "id")
internal class Cgbum(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.CGBUM, pageSize = 24) {

	override val configKeyDomain = ConfigKey.Domain("cgbum.com")
	override val availableSortOrders = setOf(SortOrder.UPDATED, SortOrder.ADDED, SortOrder.ADDED_ASC, SortOrder.POPULARITY)
	override val filterCapabilities = MangaListFilterCapabilities(
		isSearchSupported = true,
		isMultipleTagsSupported = true,
	)

	override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
		.set("Referer", "https://$domain/").build()

	override suspend fun getFilterOptions(): MangaListFilterOptions {
		val doc = webClient.httpGet("https://$domain/daftar-komik").parseHtml()
		return MangaListFilterOptions(
			availableTags = doc.select("input[name=\"genres[]\"]").mapNotNullToSet { input ->
				val value = input.attr("value").takeIf(String::isNotBlank) ?: return@mapNotNullToSet null
				MangaTag(key = value, title = value, source = source)
			},
			availableStates = setOf(MangaState.ONGOING, MangaState.FINISHED),
			availableContentTypes = setOf(ContentType.MANGA, ContentType.MANHWA, ContentType.MANHUA, ContentType.HENTAI),
		)
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val query = filter.query?.takeIf(String::isNotBlank)
		val url = "https://$domain/${if (query == null) "daftar-komik" else "cari"}".toHttpUrl().newBuilder()
			.addQueryParameter("page", page.toString())
		if (query != null) {
			url.addQueryParameter("q", query)
		} else {
			url.addQueryParameter("sort", when (order) {
				SortOrder.ADDED -> "newest"
				SortOrder.ADDED_ASC -> "oldest"
				SortOrder.POPULARITY -> "views"
				else -> "latest"
			})
			filter.types.oneOrThrowIfMany()?.let { type ->
				url.addQueryParameter("type", when (type) {
					ContentType.MANGA -> "manga"
					ContentType.MANHWA -> "manhwa"
					ContentType.MANHUA -> "manhua"
					ContentType.HENTAI -> "pornhwa"
					else -> return@let
				})
			}
			filter.states.oneOrThrowIfMany()?.let { state ->
				url.addQueryParameter("status", when (state) {
					MangaState.ONGOING -> "ongoing"
					MangaState.FINISHED -> "tamat"
					else -> return@let
				})
			}
			filter.tags.forEach { url.addQueryParameter("genres[]", it.key) }
		}
		return webClient.httpGet(url.build()).parseHtml().select("article.comic-card").mapNotNull(::parseCard)
	}

	private fun parseCard(card: Element): Manga? {
		val link = card.selectFirst("a.comic-card-cover") ?: return null
		val href = link.attrAsRelativeUrl("href")
		val title = card.selectFirst(".comic-card-title a")?.textOrNull() ?: return null
		val adult = card.selectFirst(".badge-pornhwa") != null
		return Manga(
			id = generateUid(href), url = href, publicUrl = href.toAbsoluteUrl(domain), title = title,
			altTitles = emptySet(), coverUrl = card.selectFirst("img")?.src()?.toAbsoluteUrl(domain),
			authors = emptySet(), tags = emptySet(), rating = RATING_UNKNOWN,
			state = parseState(card.selectFirst(".badge-status")?.text()), source = source,
			contentRating = if (adult) ContentRating.ADULT else null,
		)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val tags = doc.select(".comic-genres .genre-pill").mapNotNullToSet { tag ->
			val name = tag.textOrNull() ?: return@mapNotNullToSet null
			MangaTag(key = name, title = name, source = source)
		}
		val author = doc.select(".meta-row").firstOrNull {
			it.selectFirst(".meta-label")?.text()?.trimEnd(':')?.equals("Author", true) == true
		}?.selectFirst(".meta-value")?.textOrNull()?.takeUnless { it == "-" }
		val adult = doc.selectFirst(".badge-pornhwa") != null ||
			tags.any { it.title.lowercase() in setOf("adult", "hentai", "pornhwa", "18+") }
		return manga.copy(
			title = doc.selectFirst("h1")?.textOrNull() ?: manga.title,
			coverUrl = doc.selectFirst(".comic-cover img")?.src()?.toAbsoluteUrl(domain) ?: manga.coverUrl,
			description = doc.selectFirst(".comic-synopsis .synopsis-content")?.html(),
			tags = tags.ifEmpty { manga.tags }, authors = setOfNotNull(author).ifEmpty { manga.authors },
			state = parseState(doc.selectFirst(".badge-status")?.text()) ?: manga.state,
			contentRating = if (adult) ContentRating.ADULT else manga.contentRating,
			chapters = doc.select(".chapter-grid a.ch-grid-item").distinctBy { it.attr("href") }.map { a ->
				val href = a.attrAsRelativeUrl("href")
				val label = a.text()
				MangaChapter(
					id = generateUid(href), url = href, title = label, number = label.extractChapterNumber(),
					volume = 0, uploadDate = 0, source = source, scanlator = null, branch = null,
				)
			}.sortedBy { it.number },
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val chapterUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(chapterUrl).parseHtml()
		val urls = doc.select(".page-container").mapNotNull { element ->
			element.attrOrNull("data-url") ?: element.selectFirst("img")?.src()
		}.map { it.toAbsoluteUrl(domain) }.distinct()
		if (urls.isEmpty()) throw ParseException("Reader image URLs not found", chapterUrl)
		return urls.map { MangaPage(id = generateUid(it), url = it, preview = null, source = source) }
	}

	override suspend fun getRelatedManga(seed: Manga): List<Manga> {
		val doc = webClient.httpGet(seed.url.toAbsoluteUrl(domain)).parseHtml()
		return doc.select("section:has(.section-header h2:contains(Baca Juga)) article.comic-card").mapNotNull(::parseCard)
	}

	private fun parseState(value: String?) = when (value?.lowercase()) {
		"ongoing" -> MangaState.ONGOING
		"tamat" -> MangaState.FINISHED
		else -> null
	}
}

package org.koitharu.kotatsu.parsers.site.all

import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.AbstractMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*

/** Each language site publishes one work and assigns durable IDs to its archive sections. */
internal abstract class TwokindsParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : AbstractMangaParser(context, source) {

	override val configKeyDomain = ConfigKey.Domain(defaultDomain)
	override val availableSortOrders = setOf(SortOrder.UPDATED)
	override val filterCapabilities = MangaListFilterCapabilities(isSearchSupported = true)
	override suspend fun getFilterOptions() = MangaListFilterOptions()

	override suspend fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (offset != 0 || filter.query?.let {
			!"Twokinds".contains(it, true) && !"Two Kinds".contains(it, true)
		} == true) return emptyList()
		val doc = archive()
		return listOf(Manga(
			id = generateUid("/archive/"), url = "/archive/", publicUrl = "https://$domain/",
			title = "Twokinds", altTitles = setOf("Two Kinds"), rating = RATING_UNKNOWN,
			contentRating = null, coverUrl = cover(doc), tags = emptySet(), state = null,
			authors = emptySet(), source = source,
		))
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = archive()
		val chapters = doc.select("section[data-ch-id]").mapNotNull { section ->
			val chapterId = section.attr("data-ch-id").toIntOrNull() ?: return@mapNotNull null
			if (section.select("a[href^=/comic/]").isEmpty()) return@mapNotNull null
			val url = "/archive/#chapter=$chapterId"
			val title = section.selectFirst("h2")?.textOrNull()
			val primary = title?.let { Regex("(?iu)(?:chapter|глава)\\s+(\\d+)").find(it)?.groupValues?.get(1) }
			val part = title?.let { Regex("(?iu)(?:part|часть)\\s+(\\d+)").find(it)?.groupValues?.get(1) }
			val number = if (primary == null) 0f else "$primary.${part ?: "0"}".toFloatOrNull() ?: 0f
			MangaChapter(
				id = generateUid(url), url = url, title = title, number = number, volume = 0,
				uploadDate = 0, scanlator = null, branch = null, source = source,
			)
		}
		if (chapters.isEmpty()) throw ParseException("Comic archive chapters not found", "https://$domain/archive/")
		return manga.copy(
			coverUrl = cover(doc) ?: manga.coverUrl,
			description = doc.selectFirst("meta[name=description]")?.attrOrNull("content") ?: manga.description,
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val chapterId = chapter.url.substringAfter("#chapter=", "").toIntOrNull()
			?: throw ParseException("Invalid archive chapter reference", chapter.url)
		val section = archive().selectFirst("section[data-ch-id=$chapterId]")
			?: throw ParseException("Archive chapter not found", chapter.url)
		val pages = section.select("a[href^=/comic/]").distinctBy { it.attr("href") }.map { a ->
			val url = a.attrAsRelativeUrl("href")
			MangaPage(id = generateUid(url), url = url, preview = a.selectFirst("img")?.src()?.toAbsoluteUrl(domain), source = source)
		}
		if (pages.isEmpty()) throw ParseException("Archive chapter pages not found", chapter.url)
		return pages
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		val url = page.url.toAbsoluteUrl(domain)
		return webClient.httpGet(url).parseHtml().selectFirstOrThrow(".comic img, #comic img")
			.requireSrc().toAbsoluteUrl(domain)
	}

	override suspend fun getRelatedManga(seed: Manga): List<Manga> = emptyList()
	private suspend fun archive() = webClient.httpGet("https://$domain/archive/").parseHtml()
	private fun cover(doc: Document) = doc.selectFirst("section[data-ch-id] a[href^=/comic/] img")?.src()?.toAbsoluteUrl(domain)
}

@MangaSourceParser("TWOKINDS_EN", "Twokinds", "en", ContentType.COMICS)
internal class TwokindsEnglish(context: MangaLoaderContext) :
	TwokindsParser(context, MangaParserSource.TWOKINDS_EN, "twokinds.keenspot.com")

@MangaSourceParser("TWOKINDS_RU", "Twokinds на русском", "ru", ContentType.COMICS)
internal class TwokindsRussian(context: MangaLoaderContext) :
	TwokindsParser(context, MangaParserSource.TWOKINDS_RU, "twokinds.ru")

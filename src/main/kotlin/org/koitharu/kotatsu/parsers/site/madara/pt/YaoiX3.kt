package org.koitharu.kotatsu.parsers.site.madara.pt

import okhttp3.Headers
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser
import org.koitharu.kotatsu.parsers.util.oneOrThrowIfMany
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.urlEncoded
import java.util.EnumSet

@MangaSourceParser("YAOIX3", "3XYaoi", "pt", ContentType.HENTAI)
internal class YaoiX3(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YAOIX3, "3xyaoi.com") {

	override val datePattern = "dd/MM/yyyy"
	override val listUrl = "bl/"
	override val tagPrefix = "genero/"

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.POPULARITY,
		SortOrder.UPDATED,
		SortOrder.NEWEST,
		SortOrder.ALPHABETICAL,
		SortOrder.RATING,
		SortOrder.RELEVANCE,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = false,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = fetchAvailableTags(),
		availableStates = EnumSet.of(MangaState.FINISHED),
	)

	override fun getRequestHeaders(): Headers = super.getRequestHeaders().newBuilder()
		.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
		.set("Upgrade-Insecure-Requests", "1")
		.set("Sec-GPC", "1")
		.set("Sec-Fetch-User", "?1")
		.set("Sec-Fetch-Site", "none")
		.set("Sec-Fetch-Mode", "navigate")
		.set("Sec-Fetch-Dest", "document")
		.set("Priority", "u=0, i")
		.set("Pragma", "no-cache")
		.build()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildYaoiX3ListUrl(domain, page, order, filter)
		return parseMangaList(webClient.httpGet(url).parseHtml())
	}
}

internal fun buildYaoiX3ListUrl(
	domain: String,
	page: Int,
	order: SortOrder,
	filter: MangaListFilter,
): String {
	val pageNumber = page + 1
	val query = filter.query?.trim().orEmpty()
	val path = when {
		query.isNotEmpty() -> ""
		MangaState.FINISHED in filter.states -> "end/"
		filter.tags.isNotEmpty() -> "genero/${filter.tags.oneOrThrowIfMany()?.key}/"
		else -> "bl/"
	}
	return buildString {
		append("https://")
		append(domain)
		append("/")
		append(path)
		if (pageNumber > 1) {
			append("page/")
			append(pageNumber)
			append("/")
		}
		val parameters = buildList {
			if (query.isNotEmpty()) {
				add("s=${query.urlEncoded()}")
				add("post_type=wp-manga")
			}
			when (order) {
				SortOrder.POPULARITY,
				SortOrder.POPULARITY_ASC -> add("m_orderby=views")
				SortOrder.UPDATED,
				SortOrder.UPDATED_ASC -> add("m_orderby=latest")
				SortOrder.NEWEST,
				SortOrder.NEWEST_ASC -> add("m_orderby=new-manga")
				SortOrder.ALPHABETICAL,
				SortOrder.ALPHABETICAL_DESC -> add("m_orderby=alphabet")
				SortOrder.RATING,
				SortOrder.RATING_ASC -> add("m_orderby=rating")
				else -> Unit
			}
		}
		if (parameters.isNotEmpty()) {
			append("?")
			append(parameters.joinToString("&"))
		}
	}
}

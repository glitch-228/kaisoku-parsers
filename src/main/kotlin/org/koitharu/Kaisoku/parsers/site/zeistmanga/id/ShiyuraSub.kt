package org.koitharu.Kaisoku.parsers.site.zeistmanga.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.model.MangaTag
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser
import org.koitharu.Kaisoku.parsers.util.mapToSet
import org.koitharu.Kaisoku.parsers.util.parseHtml

@MangaSourceParser("SHIYURASUB", "ShiyuraSub", "id")
internal class ShiyuraSub(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.SHIYURASUB, "shiyurasub.blogspot.com") {

	override val selectTags = ".leading-8 div.my-5.gap-2 a"

	override suspend fun fetchAvailableTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain").parseHtml()
		return doc.select("div.list-label-widget-content ul li a").mapToSet {
			MangaTag(
				key = it.attr("href").removeSuffix("/").substringAfterLast('/'),
				title = it.html().substringBefore("<span"),
				source = source,
			)
		}
	}

}

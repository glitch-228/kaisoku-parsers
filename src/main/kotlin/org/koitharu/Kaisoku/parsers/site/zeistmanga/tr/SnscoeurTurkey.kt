package org.koitharu.Kaisoku.parsers.site.zeistmanga.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.model.MangaTag
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser
import org.koitharu.Kaisoku.parsers.util.mapToSet
import org.koitharu.Kaisoku.parsers.util.parseHtml
import org.koitharu.Kaisoku.parsers.util.selectFirstOrThrow

@MangaSourceParser("SNSCOEURTURKEY", "SnscoeurTurkey", "tr", ContentType.HENTAI)
internal class SnscoeurTurkey(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.SNSCOEURTURKEY, "snscoeurturkey.blogspot.com") {
	override val sateOngoing: String = "Güncel"
	override val sateFinished: String = "Final"
	override val sateAbandoned: String = "Düzenleniyor"

	override val mangaCategory = "Seriler"

	override suspend fun fetchAvailableTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain/p/gelismis-arama.html").parseHtml()
		return doc.selectFirstOrThrow("div.filter").select("ul li").mapToSet {
			MangaTag(
				key = it.selectFirstOrThrow("input").attr("value"),
				title = it.selectFirstOrThrow("label").text(),
				source = source,
			)
		}
	}
}

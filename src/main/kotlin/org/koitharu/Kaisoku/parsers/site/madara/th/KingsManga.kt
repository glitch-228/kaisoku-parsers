package org.koitharu.Kaisoku.parsers.site.madara.th

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("KINGS_MANGA", "SuperDoujin", "th", ContentType.HENTAI)
internal class KingsManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KINGS_MANGA, "www.superdoujin.org") {
	override val postReq = true
	override val datePattern = "d MMMM yyyy"
}

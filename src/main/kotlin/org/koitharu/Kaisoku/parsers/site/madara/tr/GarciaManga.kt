package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("GARCIAMANGA", "GarciaManga", "tr")
internal class GarciaManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GARCIAMANGA, "garciamanga.com", 20) {
	override val selectPage = "img"
}

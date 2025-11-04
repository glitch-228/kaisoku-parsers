package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("FLOWERMANGA", "FlowerManga", "pt")
internal class FlowerManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.FLOWERMANGA, "flowermanga.net", 24) {
	override val datePattern = "d MMMM yyyy"
}

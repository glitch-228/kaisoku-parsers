package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TOONILY", "Toonily", "en")
internal class Toonily(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TOONILY, "toonily.com", pageSize = 18) {
	override val listUrl = "webtoon/"
	override val tagPrefix = "webtoon-genre/"
	override val datePattern = "MMMM dd, yyyy"
}

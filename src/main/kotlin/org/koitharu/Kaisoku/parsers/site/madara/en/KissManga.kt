package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("KISSMANGA", "KissManga", "en")
internal class KissManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KISSMANGA, "kissmanga.in") {
	override val datePattern = "MMMM dd, yyyy"
	override val listUrl = "mangalist/"
}

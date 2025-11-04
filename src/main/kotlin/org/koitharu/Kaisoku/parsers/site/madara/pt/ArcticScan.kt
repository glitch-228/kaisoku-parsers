package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("ARCTICSCAN", "ArcticScan", "pt")
internal class ArcticScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ARCTICSCAN, "arcticscan.top") {
	override val datePattern: String = "yyyy-MM-dd"
}

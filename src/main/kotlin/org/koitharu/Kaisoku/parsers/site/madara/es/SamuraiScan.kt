package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("SAMURAISCAN", "SamuraiScan", "es")
internal class SamuraiScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.SAMURAISCAN, "samuraiscan.com", 10) {
	override val listUrl = "read/"
}

package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("HERENSCAN", "HerenScan", "es")
internal class HerenScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HERENSCAN, "herenscan.com") {
	override val datePattern = "dd/MM/yyyy"
}

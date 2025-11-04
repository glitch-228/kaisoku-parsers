package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("WONDERLANDSCAN", "WonderlandScan", "pt")
internal class WonderlandScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.WONDERLANDSCAN, "wonderlandscan.com") {
	override val datePattern: String = "dd/MM/yyyy"
}

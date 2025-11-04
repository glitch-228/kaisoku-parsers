package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("PASSAMAOSCAN", "PassamaoScan", "pt")
internal class PassamaoScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PASSAMAOSCAN, "passamaoscan.com") {
	override val datePattern: String = "dd/MM/yyyy"
}

package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("YCSCAN", "YcScan", "pt", ContentType.HENTAI)
internal class Ycscan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YCSCAN, "ycscan.com", 20) {
	override val datePattern: String = "dd/MM/yyyy"
}

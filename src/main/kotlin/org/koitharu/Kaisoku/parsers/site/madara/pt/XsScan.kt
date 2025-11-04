package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("XSSCAN", "XsScan", "pt")
internal class XsScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.XSSCAN, "xsscan.xyz") {
	override val datePattern: String = "dd/MM/yyyy"
}

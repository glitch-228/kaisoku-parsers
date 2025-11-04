package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("REMANGAS", "ReMangas", "pt")
internal class ReMangas(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.REMANGAS, "remangas.net") {
	override val datePattern = "dd/MM/yyyy"
}

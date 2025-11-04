package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("PIRULITOROSA", "PirulitoRosa", "pt", ContentType.HENTAI)
internal class Pirulitorosa(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PIRULITOROSA, "pirulitorosa.site") {
	override val postReq = true
	override val datePattern: String = "dd/MM/yyyy"
}

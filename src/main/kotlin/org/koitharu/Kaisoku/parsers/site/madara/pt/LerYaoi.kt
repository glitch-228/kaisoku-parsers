package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("LERYAOI", "LerYaoi", "pt", ContentType.HENTAI)
internal class LerYaoi(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LERYAOI, "leryaoi.com", 10) {
	override val datePattern = "dd/MM/yyyy"
	override val listUrl = "bl/"
	override val tagPrefix = "genero/"
}

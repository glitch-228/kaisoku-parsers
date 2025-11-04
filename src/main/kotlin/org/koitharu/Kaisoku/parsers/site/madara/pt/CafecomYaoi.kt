package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("CAFECOMYAOI", "CafecomYaoi", "pt", ContentType.HENTAI)
internal class CafecomYaoi(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.CAFECOMYAOI, "cafecomyaoi.com.br") {
	override val datePattern = "dd/MM/yyyy"
}

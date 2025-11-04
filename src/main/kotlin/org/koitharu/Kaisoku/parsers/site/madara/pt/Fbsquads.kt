package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("FBSQUADS", "FbSquads", "pt", ContentType.HENTAI)
internal class Fbsquads(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.FBSQUADS, "fbsquadx.com") {
	override val datePattern: String = "dd/MM/yyyy"
}

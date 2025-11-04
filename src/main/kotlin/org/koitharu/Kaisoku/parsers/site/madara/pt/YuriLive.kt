package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("YURILIVE", "YuriLive", "pt", ContentType.HENTAI)
internal class YuriLive(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YURILIVE, "yuri.live") {
	override val tagPrefix = "manga-genero/"
	override val datePattern: String = "dd/MM/yyyy"
}

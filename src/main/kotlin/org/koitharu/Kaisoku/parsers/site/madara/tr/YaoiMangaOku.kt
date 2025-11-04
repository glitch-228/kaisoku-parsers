package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("YAOIMANGAOKU", "YaoiMangaOku", "tr", ContentType.HENTAI)
internal class YaoiMangaOku(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YAOIMANGAOKU, "yaoimangaoku.net", 16) {
	override val datePattern = "d MMMM yyyy"
}

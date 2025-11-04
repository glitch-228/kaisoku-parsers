package org.koitharu.Kaisoku.parsers.site.madara.th

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGADEEMAK", "MangaDeemak", "th")
internal class Mangadeemak(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGADEEMAK, "mangadeemak.com", 12) {
	override val datePattern: String = "d MMMM yyyy"
}

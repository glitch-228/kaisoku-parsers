package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("ATIKROST", "Atikrost", "tr")
internal class Atikrost(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ATIKROST, "www.mangaoku.org", 10) {
	override val datePattern = "d MMMM yyyy"
}

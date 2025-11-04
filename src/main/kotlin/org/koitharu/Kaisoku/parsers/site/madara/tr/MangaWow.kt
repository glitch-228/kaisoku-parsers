package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAWOW", "MangaWow", "tr")
internal class MangaWow(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAWOW, "mangawow.org", 18) {
	override val datePattern = "d MMMM yyyy"
}

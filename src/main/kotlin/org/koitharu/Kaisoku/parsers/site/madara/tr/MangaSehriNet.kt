package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGASEHRINET", "MangaSehri.net", "tr")
internal class MangaSehriNet(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGASEHRINET, "manga-sehri.net", 20) {
	override val datePattern = "dd MMMM yyyy"
}

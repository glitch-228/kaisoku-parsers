package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGAOKUSANA", "MangaOkusana", "tr")
internal class MangaOkusana(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAOKUSANA, "mangaokusana.com") {
	override val datePattern = "dd MMMM yyyy"
}

package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("ZAMANMANGA", "ZamanManga", "tr")
internal class ZamanManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ZAMANMANGA, "zamanmanga.com") {
	override val datePattern = "dd MMMM yyyy"
}
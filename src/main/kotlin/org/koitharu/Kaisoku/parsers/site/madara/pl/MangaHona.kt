package org.koitharu.Kaisoku.parsers.site.madara.pl

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAHONA", "MangaHona", "pl")
internal class MangaHona(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAHONA, "mangahona.pl") {
	override val datePattern = "yyyy-MM-dd"
}

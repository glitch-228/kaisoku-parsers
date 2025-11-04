package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("FREEMANGA", "FreeManga", "en")
internal class FreeManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.FREEMANGA, "freemanga.me") {
	override val datePattern = "MMMM dd, yyyy"
}

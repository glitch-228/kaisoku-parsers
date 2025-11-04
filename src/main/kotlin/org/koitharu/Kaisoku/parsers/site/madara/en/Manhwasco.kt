package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHWASCO", "ManhwaSco", "en")
internal class Manhwasco(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHWASCO, "manhwasco.net") {
	override val selectGenre = "div.mg_genres a"
}

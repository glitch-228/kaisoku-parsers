package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("NEROXUS", "Neroxus", "pt")
internal class Neroxus(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NEROXUS, "neroxus.com.br", 10) {
	override val datePattern = "MMM d, yyyy"
}

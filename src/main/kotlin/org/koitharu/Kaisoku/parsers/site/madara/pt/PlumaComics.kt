package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("PLUMACOMICS", "PlumaComics", "pt")
internal class PlumaComics(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PLUMACOMICS, "plumacomics.cloud") {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}

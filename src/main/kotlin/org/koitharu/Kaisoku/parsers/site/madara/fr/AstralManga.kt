package org.koitharu.Kaisoku.parsers.site.madara.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken("Website, come back soon")
@MangaSourceParser("ASTRALMANGA", "AstralManga", "fr")
internal class AstralManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ASTRALMANGA, "astral-manga.fr") {
	override val datePattern = "dd/MM/yyyy"
}

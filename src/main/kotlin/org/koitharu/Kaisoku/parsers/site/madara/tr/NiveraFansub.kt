package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("NIVERAFANSUB", "NiveraFansub", "tr", ContentType.HENTAI)
internal class NiveraFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NIVERAFANSUB, "niverafansub.org") {
	override val datePattern = "d MMMM yyyy"
}

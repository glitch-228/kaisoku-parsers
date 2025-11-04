package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAREADCO", "MangaRead.co", "en")
internal class MangaReadCo(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAREADCO, "mangaread.co", 16) {
	override val tagPrefix = "m-genre/"
	override val datePattern = "yyyy-MM-dd"
}

package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAFREAK", "MangaFreak", "en")
internal class MangaFreak(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAFREAK, "mangafreak.online") {
	override val postReq = true
	override val datePattern = "dd MMMM، yyyy"
}

package org.koitharu.Kaisoku.parsers.site.madara.ru

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAONELOVE", "MangaOneLove", "ru")
internal class MangaoneLove(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAONELOVE, "mangaonelove.site", 10) {
	override val datePattern = "dd.MM.yyyy"
	override val postReq = true
}

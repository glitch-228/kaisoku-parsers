package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("ZANDYNOFANSUB", "Zandyno Fansub", "en")
internal class ZandynoFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ZANDYNOFANSUB, "zandynofansub.aishiteru.org", 20) {
	override val listUrl = "series/"
	override val datePattern = "dd.MM.yyyy"
}

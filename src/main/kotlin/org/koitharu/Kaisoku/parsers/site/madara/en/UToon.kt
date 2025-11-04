package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("UTOON", "UToon", "en")
internal class UToon(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.UTOON, "utoon.net") {
	override val datePattern = "dd MMM"
}

package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TOPREADMANHWA", "TopReadManhwa", "en")
internal class TopReadManhwa(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TOPREADMANHWA, "topreadmanhwa.com") {
	override val datePattern = "MM/dd/yyyy"
}

package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TCBSCANSMANGA", "TcbScansManga", "en")
internal class TcbScansManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TCBSCANSMANGA, "tcbscans-manga.com", 10) {
	override val selectPage = "img"
}

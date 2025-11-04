package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MURIMSCAN", "InkReads", "en")
internal class MurimScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MURIMSCAN, "inkreads.com", 100) {
	override val postReq = true
	override val listUrl = "mangax/"
}

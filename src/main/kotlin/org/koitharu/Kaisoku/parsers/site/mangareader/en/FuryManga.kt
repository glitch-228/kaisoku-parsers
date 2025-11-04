package org.koitharu.Kaisoku.parsers.site.mangareader.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("FURYMANGA", "KingOfScans", "en")
internal class FuryManga(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.FURYMANGA, "myshojo.com", pageSize = 30, searchPageSize = 10) {
	override val listUrl = "/comics"
}

package org.koitharu.Kaisoku.parsers.site.mangareader.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("ELARCPAGE", "ElarcPage", "en")
internal class Elarcpage(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.ELARCPAGE, "elarctoon.com", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/series"
}

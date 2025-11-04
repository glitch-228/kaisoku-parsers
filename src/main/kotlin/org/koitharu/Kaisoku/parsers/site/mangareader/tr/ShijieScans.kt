package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("SHIJIESCANS", "ShijieScans", "tr")
internal class ShijieScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.SHIJIESCANS, "shijiescans.com", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/seri"
}

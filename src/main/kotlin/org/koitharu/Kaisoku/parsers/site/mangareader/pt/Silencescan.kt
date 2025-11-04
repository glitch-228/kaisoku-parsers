package org.koitharu.Kaisoku.parsers.site.mangareader.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("SILENCESCAN", "SilenceScan", "pt", ContentType.HENTAI)
internal class Silencescan(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.SILENCESCAN,
		"silencescan.com.br",
		pageSize = 35,
		searchPageSize = 35,
	) {
	override val datePattern = "MMM d, yyyy"
}

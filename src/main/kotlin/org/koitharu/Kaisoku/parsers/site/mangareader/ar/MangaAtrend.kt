package org.koitharu.Kaisoku.parsers.site.mangareader.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANGAATREND", "MangaAtrend", "ar")
internal class MangaAtrend(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGAATREND, "mangaatrend.net", pageSize = 30, searchPageSize = 10)

package org.koitharu.Kaisoku.parsers.site.mangareader.ar

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("NOONSCAN", "NoonScan.com", "ar")
internal class NoonScan(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.NOONSCAN, "noonscan.com", pageSize = 20, searchPageSize = 10)

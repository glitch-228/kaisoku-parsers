package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANGAOKUTR", "MangaOkuTr", "tr")
internal class Mangaokutr(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGAOKUTR, "mangaokutr.com", pageSize = 25, searchPageSize = 20)

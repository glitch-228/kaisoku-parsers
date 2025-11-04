package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("MANGASIGINAGI", "MangaSiginagi", "tr")
internal class MangaSiginagi(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGASIGINAGI, "mangasiginagi.com", pageSize = 20, searchPageSize = 10)

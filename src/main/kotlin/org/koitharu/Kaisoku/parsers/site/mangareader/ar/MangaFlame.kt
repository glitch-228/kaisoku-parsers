package org.koitharu.Kaisoku.parsers.site.mangareader.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANGAFLAME", "MangaFlame", "ar")
internal class MangaFlame(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGAFLAME, "mangaflame.org", pageSize = 20, searchPageSize = 10)

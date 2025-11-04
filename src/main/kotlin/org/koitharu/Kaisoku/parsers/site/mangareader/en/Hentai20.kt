package org.koitharu.Kaisoku.parsers.site.mangareader.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("HENTAI20", "Hentai20", "en", ContentType.HENTAI)
internal class Hentai20(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.HENTAI20, "hentai20.io", pageSize = 20, searchPageSize = 10)

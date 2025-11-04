package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("YUMEKOMIK", "YumeKomik", "id")
internal class YumeKomik(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.YUMEKOMIK, "yumekomik.com", pageSize = 20, searchPageSize = 10)

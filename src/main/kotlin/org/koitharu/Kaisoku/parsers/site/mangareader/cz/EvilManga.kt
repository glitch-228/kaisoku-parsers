package org.koitharu.Kaisoku.parsers.site.mangareader.cz

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("EVILMANGA", "EvilManga", "cs", ContentType.HENTAI)
internal class EvilManga(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.EVILMANGA, "evil-manga.eu", pageSize = 20, searchPageSize = 10) {
	override val datePattern = "d MMMM, yyyy"
}

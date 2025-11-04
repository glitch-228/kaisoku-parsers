package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("DUNIAKOMIK", "DuniaKomik", "id", ContentType.HENTAI)
internal class Duniakomik(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.DUNIAKOMIK, "duniakomik.org", pageSize = 12, searchPageSize = 12) {
	override val datePattern = "MMM d, yyyy"
}
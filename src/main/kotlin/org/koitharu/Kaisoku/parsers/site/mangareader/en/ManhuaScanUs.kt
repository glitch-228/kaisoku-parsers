package org.koitharu.Kaisoku.parsers.site.mangareader.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANHUASCANUS", "ManhuaScan.Us", "en", ContentType.HENTAI)
internal class ManhuaScanUs(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANHUASCANUS, "manhuascan.us", pageSize = 30, searchPageSize = 30) {
	override val datePattern = "dd-MM-yyyy"
	override val listUrl = "/manga-list"
}

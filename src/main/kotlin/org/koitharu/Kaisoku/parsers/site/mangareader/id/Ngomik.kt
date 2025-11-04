package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import java.util.*

@MangaSourceParser("NGOMIK", "Ngomik", "id")
internal class Ngomik(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.NGOMIK, "ngomik.mom", pageSize = 20, searchPageSize = 5) {
	override val sourceLocale: Locale = Locale.ENGLISH
}

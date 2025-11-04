package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import java.util.*

@MangaSourceParser("KOFISCANS", "KofiScans", "id")
internal class KofiScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOFISCANS, "isekaikomik.com", pageSize = 20, searchPageSize = 10) {
	override val sourceLocale: Locale = Locale.ENGLISH
}

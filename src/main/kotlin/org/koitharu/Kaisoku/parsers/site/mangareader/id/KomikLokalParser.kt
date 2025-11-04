package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import org.koitharu.Kaisoku.parsers.Broken
import java.util.*

@Broken
@MangaSourceParser("KOMIKLOKAL", "KomikLokal", "id")
internal class KomikLokalParser(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOMIKLOKAL, "komikmu.top", pageSize = 20, searchPageSize = 10) {
	override val datePattern = "MMM d, yyyy"
	override val sourceLocale: Locale = Locale.ENGLISH
}

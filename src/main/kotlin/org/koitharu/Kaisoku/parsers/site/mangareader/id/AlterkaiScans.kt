package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import java.util.*

@MangaSourceParser("ALTERKAISCANS", "AlterkaiScans", "id")
internal class AlterkaiScans(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.ALTERKAISCANS,
		"alterkaiscans.my.id",
		pageSize = 20,
		searchPageSize = 10,
	) {
	override val sourceLocale: Locale = Locale.ENGLISH
}

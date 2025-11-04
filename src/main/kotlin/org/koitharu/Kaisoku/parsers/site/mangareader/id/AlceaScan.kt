package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import java.util.*

@Broken
@MangaSourceParser("ALCEASCAN", "AlceaScan", "id")
internal class AlceaScan(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.ALCEASCAN, "alceacomic.my.id", pageSize = 20, searchPageSize = 10) {
	override val sourceLocale: Locale = Locale.ENGLISH
}
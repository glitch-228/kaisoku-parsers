package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import java.util.*
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Not dead, changed template")
@MangaSourceParser("WESTMANGA", "WestManga", "id")
internal class WestmangaParser(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.WESTMANGA, "westmanga.me", pageSize = 20, searchPageSize = 10) {
	override val sourceLocale: Locale = Locale.ENGLISH
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

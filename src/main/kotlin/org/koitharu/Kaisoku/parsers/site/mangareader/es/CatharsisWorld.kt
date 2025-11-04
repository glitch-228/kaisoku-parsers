package org.koitharu.Kaisoku.parsers.site.mangareader.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import java.util.Locale
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Not dead, changed template")
@MangaSourceParser("CATHARSISWORLD", "CatharsisWorld", "es")
internal class CatharsisWorld(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.CATHARSISWORLD, "catharsisworld.dig-it.info", pageSize = 30, searchPageSize = 10) {
	override val sourceLocale: Locale = Locale.ENGLISH
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

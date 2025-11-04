package org.koitharu.Kaisoku.parsers.site.mangareader.pl

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("SKANLACJEFENIKSY", "SkanlacjeFeniksy", "pl")
internal class SkanlacjeFeniksy(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.SKANLACJEFENIKSY,
		"skanlacje-feniksy.pl",
		pageSize = 10,
		searchPageSize = 10,
	) {
	override val datePattern = "d MMMM, yyyy"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

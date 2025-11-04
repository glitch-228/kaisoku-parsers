package org.koitharu.Kaisoku.parsers.site.mangareader.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("ENTHUNDERSCANS", "EnThunderScans", "en")
internal class EnThunderScans(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.ENTHUNDERSCANS,
		"en-thunderscans.com",
		pageSize = 30,
		searchPageSize = 10,
	) {
	override val listUrl = "/comics"

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

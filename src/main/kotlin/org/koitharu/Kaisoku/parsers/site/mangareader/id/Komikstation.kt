package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("KOMIKSTATION", "KomikStation", "id")
internal class Komikstation(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOMIKSTATION, "komikstation.org", pageSize = 30, searchPageSize = 30) {
	override val datePattern = "MMM d, yyyy"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

package org.koitharu.Kaisoku.parsers.site.mangareader.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("FLARES", "Fl-Ares", "ar")
internal class FlAres(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.FLARES, "fl-ares.com", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/series"
	override val encodedSrc = true
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

package org.koitharu.Kaisoku.parsers.site.mangareader.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("COSMICSCANS", "CosmicScans.com", "en")
internal class CosmicScansParser(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.COSMICSCANS, "cosmic-scans.com", pageSize = 20, searchPageSize = 10) {
	override val datePattern = "MMM d, yyyy"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

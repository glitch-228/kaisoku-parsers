package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("HYPERIONSCANS", "SeraphManga", "tr")
internal class HyperionScans(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.HYPERIONSCANS,
		"www.seraphmanga.com",
		pageSize = 20,
		searchPageSize = 10,
	) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

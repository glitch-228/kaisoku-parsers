package org.koitharu.Kaisoku.parsers.site.mangareader.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("SUSHISCANFR", "SushiScan.fr", "fr")
internal class SushiScanFR(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.SUSHISCANFR, "sushiscan.fr", pageSize = 36, searchPageSize = 10) {
	override val listUrl = "/catalogue"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

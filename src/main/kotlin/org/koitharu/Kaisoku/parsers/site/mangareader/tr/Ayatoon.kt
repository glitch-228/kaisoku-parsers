package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("AYATOON", "AyaToon", "tr")
internal class Ayatoon(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.AYATOON, "ayatoon.com", pageSize = 20, searchPageSize = 20) {

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

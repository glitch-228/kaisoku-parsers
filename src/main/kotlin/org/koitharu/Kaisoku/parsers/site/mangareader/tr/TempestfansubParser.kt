package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("TEMPESTFANSUB", "TempestFansub.Com", "tr")
internal class TempestfansubParser(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.TEMPESTFANSUB,
		"tempestscans.net",
		pageSize = 25,
		searchPageSize = 40,
	) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

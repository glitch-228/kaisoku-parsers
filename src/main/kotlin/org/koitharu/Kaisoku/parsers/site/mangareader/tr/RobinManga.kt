package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("ROBINMANGA", "RobinManga", "tr")
internal class RobinManga(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.ROBINMANGA,
		"www.guildknives.com",
		pageSize = 20,
		searchPageSize = 25,
	) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

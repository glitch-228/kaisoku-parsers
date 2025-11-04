package org.koitharu.Kaisoku.parsers.site.mangareader.th

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("MANGAMOONS", "MangaMoons", "th")
internal class MangaMoons(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGAMOONS, "manga-moons.net", pageSize = 20, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("CULTURESUBS", "CultureSubs", "tr")
internal class CultureSubs(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.CULTURESUBS, "culturesubs.com", pageSize = 20, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isMultipleTagsSupported = false,
		)
}

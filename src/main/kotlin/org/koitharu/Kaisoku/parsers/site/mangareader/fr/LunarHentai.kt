package org.koitharu.Kaisoku.parsers.site.mangareader.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("LUNARHENTAI", "GloryScans", "fr")
internal class LunarHentai(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.LUNARHENTAI, "gloryscans.fr", pageSize = 40, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

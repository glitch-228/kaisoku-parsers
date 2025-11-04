package org.koitharu.Kaisoku.parsers.site.mangareader.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("DEMONSECT", "DemonSect", "pt")
internal class DemonSect(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.DEMONSECT, "seitacelestial.com", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/comics"

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

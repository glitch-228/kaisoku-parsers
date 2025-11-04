package org.koitharu.Kaisoku.parsers.site.mangareader.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("VFSCAN", "VfScan", "fr")
internal class VfScan(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.VFSCAN, "www.vfscan.net", pageSize = 18, searchPageSize = 18) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

package org.koitharu.Kaisoku.parsers.site.mangareader.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("TRESDAOS", "Tresdaos", "es")
internal class Tresdaos(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.TRESDAOS, "threedaos.zdrz.xyz", 20, 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

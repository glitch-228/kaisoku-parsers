package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("ARCURAFANSUB", "ArcuraFansub", "tr", ContentType.HENTAI)
internal class ArcuraFansub(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.ARCURAFANSUB, "arcurafansub.com", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/seri"

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

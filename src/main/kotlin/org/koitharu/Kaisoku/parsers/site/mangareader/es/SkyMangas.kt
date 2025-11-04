package org.koitharu.Kaisoku.parsers.site.mangareader.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("SKYMANGAS", "SkyMangas", "es")
internal class SkyMangas(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.SKYMANGAS, "skymangas.com", pageSize = 20, searchPageSize = 10) {
	override val encodedSrc = true
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

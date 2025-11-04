package org.koitharu.Kaisoku.parsers.site.mangareader.th

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("NTRMANGA", "NtrManga", "th", ContentType.HENTAI)
internal class NtrManga(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.NTRMANGA, "www.ntr-manga.com", pageSize = 30, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

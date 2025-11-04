package org.koitharu.Kaisoku.parsers.site.mangareader.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("SKY_MANGA", "SkyManga", "en")
internal class SkyManga(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.SKY_MANGA, "skymanga.work", pageSize = 20, searchPageSize = 20) {
	override val listUrl = "/manga-list"
	override val datePattern = "dd-MM-yyy"
}

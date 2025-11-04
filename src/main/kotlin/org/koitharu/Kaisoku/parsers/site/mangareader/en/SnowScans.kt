package org.koitharu.Kaisoku.parsers.site.mangareader.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken("Redirect to @FLIXSCANS")
@MangaSourceParser("SNOWSCANS", "SnowScans", "en")
internal class SnowScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.SNOWSCANS, "flixscans.net", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/series"
}

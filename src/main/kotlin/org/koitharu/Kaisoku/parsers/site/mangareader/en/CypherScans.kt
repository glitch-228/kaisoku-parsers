package org.koitharu.Kaisoku.parsers.site.mangareader.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("CYPHERSCANS", "CypherScans", "en")
internal class CypherScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.CYPHERSCANS, "cypheroscans.xyz", pageSize = 20, searchPageSize = 10)

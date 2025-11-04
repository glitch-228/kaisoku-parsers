package org.koitharu.Kaisoku.parsers.site.mangareader.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("PANTHEONSCAN_FR", "PantheonScan.fr", "fr")
internal class PantheonScanFr(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.PANTHEONSCAN_FR,
		"www.pantheon-scan.fr",
		pageSize = 40,
		searchPageSize = 10,
	)

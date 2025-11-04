package org.koitharu.Kaisoku.parsers.site.mangareader.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("IRISSCANLATOR", "IrisScanlator", "pt")
internal class IrisScanlator(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.IRISSCANLATOR,
		"irisscanlator.com.br",
		pageSize = 20,
		searchPageSize = 10,
	)

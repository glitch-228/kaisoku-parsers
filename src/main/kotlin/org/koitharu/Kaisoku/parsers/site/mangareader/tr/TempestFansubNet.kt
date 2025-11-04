package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("TEMPESTFANSUBNET", "tempestmangas.com", "tr")
internal class TempestFansubNet(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.TEMPESTFANSUBNET,
		"tempestfansub.net",
		pageSize = 30,
		searchPageSize = 10,
	)

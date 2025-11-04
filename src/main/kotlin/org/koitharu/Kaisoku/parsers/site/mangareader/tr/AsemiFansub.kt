package org.koitharu.Kaisoku.parsers.site.mangareader.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("ASEMIFANSUB", "AsemiFansub", "tr")
internal class AsemiFansub(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.ASEMIFANSUB, "asemifansub.com", pageSize = 20, searchPageSize = 10)

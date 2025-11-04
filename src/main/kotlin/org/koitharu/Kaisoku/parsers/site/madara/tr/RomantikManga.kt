package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("ROMANTIKMANGA", "RomantikManga", "tr")
internal class RomantikManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ROMANTIKMANGA, "webtoonhatti.club", 20)

package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("YAOIMANGA", "YaoiManga", "es")
internal class YaoiManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YAOIMANGA, "yaoimanga.es", 42)

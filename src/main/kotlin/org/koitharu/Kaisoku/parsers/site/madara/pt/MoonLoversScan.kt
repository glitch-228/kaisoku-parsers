package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MOONLOVERSSCAN", "MoonLoversScan", "pt", ContentType.HENTAI)
internal class MoonLoversScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MOONLOVERSSCAN, "moonloversscan.com.br", 10)

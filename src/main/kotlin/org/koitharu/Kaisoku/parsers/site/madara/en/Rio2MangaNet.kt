package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("RIO2MANGANET", "ZinchanManga.mobi", "en")
internal class Rio2MangaNet(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.RIO2MANGANET, "zinchanmanga.mobi", 10)

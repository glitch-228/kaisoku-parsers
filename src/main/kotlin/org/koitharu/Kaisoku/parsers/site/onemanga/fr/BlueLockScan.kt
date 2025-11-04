package org.koitharu.Kaisoku.parsers.site.onemanga.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.onemanga.OneMangaParser

@MangaSourceParser("BLUELOCKSCAN", "BlueLockScan", "fr")
internal class BlueLockScan(context: MangaLoaderContext) :
	OneMangaParser(context, MangaParserSource.BLUELOCKSCAN, "bluelockscan.com")

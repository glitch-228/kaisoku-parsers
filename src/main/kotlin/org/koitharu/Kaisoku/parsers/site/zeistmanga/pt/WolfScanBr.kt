package org.koitharu.Kaisoku.parsers.site.zeistmanga.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("WOLFSCANBR", "WolfScanBr", "pt")
internal class WolfScanBr(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.WOLFSCANBR, "wolfscanbr.blogspot.com")

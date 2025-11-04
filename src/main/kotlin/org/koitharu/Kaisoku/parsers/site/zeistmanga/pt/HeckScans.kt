package org.koitharu.Kaisoku.parsers.site.zeistmanga.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("HECKSCANS", "HeckScans", "pt")
internal class HeckScans(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.HECKSCANS, "heckscans.blogspot.com")

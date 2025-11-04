package org.koitharu.Kaisoku.parsers.site.cupfox.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.cupfox.CupFoxParser

@MangaSourceParser("SEINEMANGA", "SeineManga", "fr")
internal class SeineManga(context: MangaLoaderContext) :
	CupFoxParser(context, MangaParserSource.SEINEMANGA, "www.seinemanga.com")

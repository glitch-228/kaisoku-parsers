package org.koitharu.Kaisoku.parsers.site.cupfox.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.cupfox.CupFoxParser

@Broken
@MangaSourceParser("ENLIGNEMANGA", "EnLigneManga", "fr")
internal class EnLigneManga(context: MangaLoaderContext) :
	CupFoxParser(context, MangaParserSource.ENLIGNEMANGA, "www.enlignemanga.com")

package org.koitharu.Kaisoku.parsers.site.cupfox.ja

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.cupfox.CupFoxParser

@MangaSourceParser("MANGAKOINU", "MangaKoinu", "ja")
internal class MangaKoinu(context: MangaLoaderContext) :
	CupFoxParser(context, MangaParserSource.MANGAKOINU, "www.mangakoinu.com")

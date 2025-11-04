package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAECLIPSE", "MangaEclipse", "en")
internal class MangaEclipse(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAECLIPSE, "mangaeclipse.com")

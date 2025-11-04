package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MI2MANGAES", "Mi2MangaEs", "es")
internal class Mi2MangaEs(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MI2MANGAES, "mi2manga.lat", 10)

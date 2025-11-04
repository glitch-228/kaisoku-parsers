package org.koitharu.Kaisoku.parsers.site.pizzareader.it

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.pizzareader.PizzaReaderParser

@MangaSourceParser("PHOENIXSCANS", "PhoenixScans", "it")
internal class PhoenixScans(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.PHOENIXSCANS, "www.phoenixscans.com")

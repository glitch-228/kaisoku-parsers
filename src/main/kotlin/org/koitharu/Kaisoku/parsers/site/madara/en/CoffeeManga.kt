package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("COFFEE_MANGA", "CoffeeManga", "en")
internal class CoffeeManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.COFFEE_MANGA, "coffeemanga.io")

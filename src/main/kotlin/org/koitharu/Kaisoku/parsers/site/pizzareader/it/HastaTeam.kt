package org.koitharu.Kaisoku.parsers.site.pizzareader.it

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.pizzareader.PizzaReaderParser

@MangaSourceParser("HASTATEAM", "HastaTeamDdt", "it")
internal class HastaTeam(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.HASTATEAM, "ddt.hastateam.com")

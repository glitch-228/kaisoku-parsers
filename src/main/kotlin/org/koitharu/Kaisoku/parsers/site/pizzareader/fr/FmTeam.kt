package org.koitharu.Kaisoku.parsers.site.pizzareader.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.pizzareader.PizzaReaderParser

@MangaSourceParser("FMTEAM", "FmTeam", "fr")
internal class FmTeam(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.FMTEAM, "fmteam.fr") {
	override val ongoingFilter = "en cours"
	override val completedFilter = "terminé"
}

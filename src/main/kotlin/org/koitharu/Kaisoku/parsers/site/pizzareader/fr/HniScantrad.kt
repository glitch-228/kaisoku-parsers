package org.koitharu.Kaisoku.parsers.site.pizzareader.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.pizzareader.PizzaReaderParser

@MangaSourceParser("HNISCANTRAD", "HniScantrad", "fr")
internal class HniScantrad(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.HNISCANTRAD, "hni-scantrad.net") {
	override val ongoingFilter = "en cours"
	override val completedFilter = "terminé"
}

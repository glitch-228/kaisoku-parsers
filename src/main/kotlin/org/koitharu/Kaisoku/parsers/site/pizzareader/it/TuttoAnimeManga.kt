package org.koitharu.Kaisoku.parsers.site.pizzareader.it

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.pizzareader.PizzaReaderParser

@MangaSourceParser("TUTTOANIMEMANGA", "TuttoAnimeManga", "it")
internal class TuttoAnimeManga(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.TUTTOANIMEMANGA, "tuttoanimemanga.net") {
	override val completedFilter = "completato"
}

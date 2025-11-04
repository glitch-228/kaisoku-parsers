package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("LEITORDEMANGA", "LeitorDeManga", "pt")
internal class LeitorDeManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LEITORDEMANGA, "leitordemanga.com", 10) {
	override val datePattern = "dd/MM/yyyy"
	override val listUrl = "ler-manga/"
}

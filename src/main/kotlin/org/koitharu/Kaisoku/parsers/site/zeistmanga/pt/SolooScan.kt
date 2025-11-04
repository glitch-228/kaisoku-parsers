package org.koitharu.Kaisoku.parsers.site.zeistmanga.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("SOLOOSCAN", "SolooScan", "pt")
internal class SolooScan(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.SOLOOSCAN, "solooscan.blogspot.com") {
	override val mangaCategory = "Recentes"
	override val sateOngoing: String = "Lançando"
	override val sateFinished: String = "Completo"
	override val sateAbandoned: String = "Dropado"
}

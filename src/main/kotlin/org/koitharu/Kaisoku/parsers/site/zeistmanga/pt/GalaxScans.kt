package org.koitharu.Kaisoku.parsers.site.zeistmanga.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("GALAXSCANS", "GalaxScanlator", "pt")
internal class GalaxScans(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.GALAXSCANS, "galaxscanlator.blogspot.com") {
	override val mangaCategory = "Recentes"
	override val sateOngoing: String = "Lançando"
	override val sateFinished: String = "Completo"
	override val sateAbandoned: String = "Dropado"
}

package org.koitharu.Kaisoku.parsers.site.zeistmanga.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("GISTAMISHOUSEFANSUB", "GistamisHouseFansub", "es")
internal class GistamisHouseFansub(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.GISTAMISHOUSEFANSUB, "gistamishousefansub.blogspot.com") {
	override val sateOngoing: String = "Activo"
	override val sateFinished: String = "Completo"
	override val sateAbandoned: String = "Cancelado"
	override val selectPage = ".post img"
}

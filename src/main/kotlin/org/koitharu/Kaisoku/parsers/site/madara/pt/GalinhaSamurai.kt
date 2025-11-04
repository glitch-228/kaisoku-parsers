package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("GALINHASAMURAI", "GalinhaSamurai", "pt")
internal class GalinhaSamurai(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GALINHASAMURAI, "galinhasamurai.com") {
	override val datePattern = "dd/MM/yyyy"
	override val withoutAjax = true
}

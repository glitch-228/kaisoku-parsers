package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("KNIGHTNOSCANLATION", "Lector KNS", "es")
internal class KnightnoScanlation(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KNIGHTNOSCANLATION, "lectorknight.com") {
	override val listUrl = "sr/"
	override val tagPrefix = "generos/"
}

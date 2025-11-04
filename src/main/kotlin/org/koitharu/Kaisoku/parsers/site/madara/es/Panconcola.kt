package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("PANCONCOLA", "Panconcola", "es")
internal class Panconcola(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PANCONCOLA, "artessupremas.com") {
	override val datePattern = "dd/MM/yyyy"
	override val tagPrefix = "generos-de-manga/"
}

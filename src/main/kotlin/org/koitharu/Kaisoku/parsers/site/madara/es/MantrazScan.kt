package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANTRAZSCAN", "MantrazScan", "es")
internal class MantrazScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANTRAZSCAN, "mantrazscan.org") {
	override val datePattern = "dd/MM/yyyy"
	override val tagPrefix = "generos-de-manga/"
}

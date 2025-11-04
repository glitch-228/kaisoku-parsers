package org.koitharu.Kaisoku.parsers.site.madara.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("OLAOE", "Olaoe", "ar")
internal class Olaoe(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.OLAOE, "olaoe.cyou") {
	override val datePattern = "dd-MM-yyyy"
	override val tagPrefix = "/شوجو"
	override val listUrl = "works/"
}

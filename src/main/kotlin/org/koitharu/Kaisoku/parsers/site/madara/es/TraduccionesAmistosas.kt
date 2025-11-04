package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TRADUCCIONESAMISTOSAS", "TraduccionesAmistosas", "es")
internal class TraduccionesAmistosas(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TRADUCCIONESAMISTOSAS, "traduccionesamistosas.topmanhuas.org", 10) {
	override val datePattern = "d 'de' MMMMM 'de' yyyy"
}

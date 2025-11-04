package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TAURUSMANGA", "TaurusManga", "es")
internal class TaurusManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TAURUSMANGA, "taurus.topmanhuas.org") {
	override val datePattern = "dd/MM/yyyy"
}

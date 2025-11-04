package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("FENIXPROJECT", "FenixProject", "pt")
internal class FenixProject(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.FENIXPROJECT, "fenixproject.site", 10) {
	override val datePattern: String = "dd/MM/yyyy"
}

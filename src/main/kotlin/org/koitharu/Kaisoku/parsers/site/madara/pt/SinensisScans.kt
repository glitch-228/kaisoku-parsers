package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken("Not dead, changed template")
@MangaSourceParser("SINENSISSCANS", "SinensisScans", "pt")
internal class SinensisScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.SINENSISSCANS, "sinensis.leitorweb.com") {
	override val datePattern: String = "dd/MM/yyyy"
}

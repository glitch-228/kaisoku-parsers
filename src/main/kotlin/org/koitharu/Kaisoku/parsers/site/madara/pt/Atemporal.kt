package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("ATEMPORAL", "Atemporal", "pt")
internal class Atemporal(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ATEMPORAL, "atemporal.cloud") {
	override val datePattern: String = "d 'de' MMMM 'de' yyyy"
	override val withoutAjax = true
}

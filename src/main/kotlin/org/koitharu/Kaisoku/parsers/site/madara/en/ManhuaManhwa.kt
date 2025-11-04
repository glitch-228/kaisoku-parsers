package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHUAMANHWA", "ManhuaManhwa", "en")
internal class ManhuaManhwa(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHUAMANHWA, "manhuamanhwa.com") {
	override val datePattern = "MM/dd/yyyy"
}

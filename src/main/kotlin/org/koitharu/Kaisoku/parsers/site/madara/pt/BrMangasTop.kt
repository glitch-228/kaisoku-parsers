package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("BRMANGASTOP", "BrMangasTop", "pt")
internal class BrMangasTop(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.BRMANGASTOP, "brmangas.top", 10) {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}

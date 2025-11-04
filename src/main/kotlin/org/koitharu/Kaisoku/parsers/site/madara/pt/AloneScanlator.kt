package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("ALONESCANLATOR", "AloneScanlator", "pt")
internal class AloneScanlator(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ALONESCANLATOR, "alonescanlator.com.br", 10) {
	override val datePattern: String = "dd/MM/yyyy"
}

package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("LEITORKAMISAMA", "LeitorKamisama", "pt")
internal class LeitorKamisama(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LEITORKAMISAMA, "leitor.kamisama.com.br", 10) {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}

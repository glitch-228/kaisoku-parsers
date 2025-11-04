package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("NOCSUMMER", "NocturneSummer", "pt", ContentType.HENTAI)
internal class Nocsummer(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NOCSUMMER, "nocfsb.com", 18) {
	override val datePattern = "dd 'de' MMMMM 'de' yyyy"
}

package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TRMANGAOKU", "TrMangaOku", "tr")
internal class TrMangaOku(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TRMANGAOKU, "trmangaoku.com") {
	override val tagPrefix = "tur/"
}

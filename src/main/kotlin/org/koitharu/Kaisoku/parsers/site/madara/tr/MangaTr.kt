package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGATR", "MangaTr", "tr")
internal class MangaTr(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGATR, "mangatr.app") {
	override val tagPrefix = "tur/"
}

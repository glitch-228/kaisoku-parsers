package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("YAOITR", "YaoiTr", "tr")
internal class YaoiTr(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YAOITR, "yaoitr.fun", 16) {
	override val datePattern = "d MMMM yyyy"
}

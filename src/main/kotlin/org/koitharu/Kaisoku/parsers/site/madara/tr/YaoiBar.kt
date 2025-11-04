package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("YAOIBAR", "YaoiBar", "tr")
internal class YaoiBar(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YAOIBAR, "yaoibar.gay", 16) {
	override val datePattern = "dd/MM/yyyy"
}

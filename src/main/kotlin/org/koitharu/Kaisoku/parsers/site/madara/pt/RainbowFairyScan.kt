package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("RAINBOWFAIRYSCAN", "RainbowFairyScan", "pt")
internal class RainbowFairyScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.RAINBOWFAIRYSCAN, "rainbowfairyscan.com", 10) {
	override val datePattern: String = "dd/MM/yyyy"
}

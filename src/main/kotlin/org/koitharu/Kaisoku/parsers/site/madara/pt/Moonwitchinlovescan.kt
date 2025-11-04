package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser


@MangaSourceParser("MOONWITCHINLOVESCAN", "MoonWitchinScan", "pt")
internal class Moonwitchinlovescan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MOONWITCHINLOVESCAN, "moonwitchscan.com.br", 10) {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}

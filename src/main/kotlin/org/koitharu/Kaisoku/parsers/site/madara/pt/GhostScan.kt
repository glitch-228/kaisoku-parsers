package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("GHOSTSCAN", "GhostScan", "pt")
internal class GhostScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GHOSTSCAN, "ghostscan.xyz", 24) {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}

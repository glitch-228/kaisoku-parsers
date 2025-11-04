package org.koitharu.Kaisoku.parsers.site.madara.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("PANTHEONSCAN", "PantheonScan.com", "fr")
internal class PantheonScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PANTHEONSCAN, "pantheon-scan.com") {
	override val datePattern = "d MMMM yyyy"
}

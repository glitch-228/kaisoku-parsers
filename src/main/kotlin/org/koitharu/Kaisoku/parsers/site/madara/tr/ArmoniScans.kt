package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("ARMONISCANS", "ArmoniScans", "tr")
internal class ArmoniScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ARMONISCANS, "armoniscans.net") {
	override val tagPrefix = "tur/"
}

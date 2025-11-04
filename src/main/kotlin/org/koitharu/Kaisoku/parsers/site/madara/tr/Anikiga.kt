package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("ANIKIGA", "Anikiga", "tr")
internal class Anikiga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ANIKIGA, "anikiga.com") {
	override val tagPrefix = "manga-tur/"
	override val datePattern = "d MMMM yyyy"
	override val postReq = true
}

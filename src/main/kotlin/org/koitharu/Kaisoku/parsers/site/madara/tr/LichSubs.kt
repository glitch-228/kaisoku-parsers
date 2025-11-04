package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("LICHSUBS", "LichSubs", "tr")
internal class LichSubs(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LICHSUBS, "www.kuroimanga.com") {
	override val datePattern = "dd/MM/yyyy"
}

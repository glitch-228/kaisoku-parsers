package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("NABISCANS", "NabiScans", "tr")
internal class NabiScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NABISCANS, "nabiscans.com") {
	override val datePattern = "d MMMM yyyy"
}

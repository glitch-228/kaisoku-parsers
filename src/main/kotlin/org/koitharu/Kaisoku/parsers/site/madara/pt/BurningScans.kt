package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("BURNINGSCANS", "BurningScans", "pt")
internal class BurningScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.BURNINGSCANS, "burningscans.com") {
	override val datePattern = "dd/MM/yyyy"
}

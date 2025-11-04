package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("THEGUILDSCANS", "TheGuildScans", "en")
internal class Theguildscans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.THEGUILDSCANS, "theguildscans.com") {
	override val postReq = true
}

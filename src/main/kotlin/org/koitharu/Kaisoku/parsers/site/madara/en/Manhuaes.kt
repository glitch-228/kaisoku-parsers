package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHUAES", "ManhuaEs", "en")
internal class Manhuaes(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHUAES, "manhuaes.com") {
	override val postReq = true
}

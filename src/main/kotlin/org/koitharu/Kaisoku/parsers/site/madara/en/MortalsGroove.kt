package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MORTALSGROOVE", "MortalsGroove", "en")
internal class MortalsGroove(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MORTALSGROOVE, "mortalsgroove.com") {
	override val postReq = true
}

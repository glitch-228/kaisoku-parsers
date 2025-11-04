package org.koitharu.Kaisoku.parsers.site.madara.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("GATEMANGA", "GateManga", "ar")
internal class GateManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GATEMANGA, "gatemanga.com") {
	override val postReq = true
	override val datePattern = "d MMMM، yyyy"
	override val listUrl = "ar/"
	override val withoutAjax = true
}

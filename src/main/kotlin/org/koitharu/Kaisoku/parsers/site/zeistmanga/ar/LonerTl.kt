package org.koitharu.Kaisoku.parsers.site.zeistmanga.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("LONERTL", "LonerTranslations", "ar")
internal class LonerTl(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.LONERTL, "loner-tl.blogspot.com") {
	override val sateOngoing: String = "مستمرة"
	override val sateFinished: String = "مكتملة"
	override val sateAbandoned: String = "متوقفة"
}

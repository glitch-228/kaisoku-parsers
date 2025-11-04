package org.koitharu.Kaisoku.parsers.site.zeistmanga.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("SHADOWCEVIRI", "ShadowCeviri", "tr", ContentType.COMICS)
internal class ShadowCeviri(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.SHADOWCEVIRI, "shadowceviri.blogspot.com") {
	override val sateOngoing: String = "Devam Ediyor"
	override val sateFinished: String = "Tamamlandı"
	override val sateAbandoned: String = "Güncel"
}

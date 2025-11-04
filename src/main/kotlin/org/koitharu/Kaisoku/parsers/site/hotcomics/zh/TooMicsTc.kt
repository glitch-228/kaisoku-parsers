package org.koitharu.Kaisoku.parsers.site.hotcomics.zh

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.hotcomics.HotComicsParser

@MangaSourceParser("TOOMICSTC", "TooMicsTc", "zh")
internal class TooMicsTc(context: MangaLoaderContext) :
	HotComicsParser(context, MangaParserSource.TOOMICSTC, "toomics.com/tc") {
	override val isSearchSupported = false
	override val mangasUrl = "/webtoon/ranking/genre"
	override val selectMangas = "li > div.visual"
	override val selectMangaChapters = "li.normal_ep:has(.coin-type1)"
	override val selectTagsList = "div.genre_list li:not(.on) a"
	override val selectPages = "div[id^=load_image_] img"
	override val onePage = true
}

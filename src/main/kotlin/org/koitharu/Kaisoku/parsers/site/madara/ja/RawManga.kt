package org.koitharu.Kaisoku.parsers.site.madara.ja

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("RAWMANGA", "RawManga", "ja")
internal class RawManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.RAWMANGA, "rawmanga.su", 24) {
	override val listUrl = "r/"
	override val selectPage = "div.mg-item"
}

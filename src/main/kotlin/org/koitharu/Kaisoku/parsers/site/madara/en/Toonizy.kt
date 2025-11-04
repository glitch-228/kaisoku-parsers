package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TOONIZY", "Toonizy", "en", ContentType.HENTAI)
internal class Toonizy(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TOONIZY, "toonizy.com", 24) {
	override val datePattern = "MMM d, yy"
	override val listUrl = "webtoon/"
}

package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("READFREECOMICS", "ReadFreeComics", "en")
internal class ReadFreeComics(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.READFREECOMICS, "readfreecomics.com") {
	override val tagPrefix = "webtoon-comic-genre/"
	override val listUrl = "webtoon-comic/"
}

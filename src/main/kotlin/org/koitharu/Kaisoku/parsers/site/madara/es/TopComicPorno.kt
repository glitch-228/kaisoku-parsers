package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TOPCOMICPORNO", "TopComicPorno", "es", ContentType.HENTAI)
internal class TopComicPorno(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TOPCOMICPORNO, "topcomicporno.net", 18) {
	override val datePattern = "MMM dd, yy"
}

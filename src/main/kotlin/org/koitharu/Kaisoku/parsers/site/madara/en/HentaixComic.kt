package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("HENTAIXCOMIC", "Hentai x Comic", "en", ContentType.HENTAI)
internal class HentaixComic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HENTAIXCOMIC, "hentaixcomic.com", 16) {
	override val postReq = true
}

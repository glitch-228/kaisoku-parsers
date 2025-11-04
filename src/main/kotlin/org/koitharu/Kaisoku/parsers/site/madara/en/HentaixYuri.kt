package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("HENTAIXYURI", "HentaiXYuri", "en", ContentType.HENTAI)
internal class HentaixYuri(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HENTAIXYURI, "hentaixyuri.com", 16) {
	override val postReq = true
}

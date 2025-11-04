package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAFORFREE", "MangaForFree", "en", ContentType.HENTAI)
internal class MangaForFree(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAFORFREE, "mangaforfree.com", 10) {
	override val postReq = true
}

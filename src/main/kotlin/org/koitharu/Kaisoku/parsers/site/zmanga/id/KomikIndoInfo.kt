package org.koitharu.Kaisoku.parsers.site.zmanga.id

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zmanga.ZMangaParser

@Broken("Redirect to @MANGASUSUKU")
@MangaSourceParser("KOMIKINDO_INFO", "KomikIndo.info", "id", ContentType.HENTAI)
internal class KomikIndoInfo(context: MangaLoaderContext) :
	ZMangaParser(context, MangaParserSource.KOMIKINDO_INFO, "mangasusuku.com") {
	override val datePattern = "dd MMM yyyy"
}

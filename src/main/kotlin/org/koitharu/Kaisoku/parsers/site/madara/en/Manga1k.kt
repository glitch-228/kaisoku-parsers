package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGA1K", "Manga1k", "en", ContentType.HENTAI)
internal class Manga1k(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA1K, "manga1k.com", 20) {
	override val withoutAjax = true
}

package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGAOWL_ONE", "MangaOwl.one", "en", ContentType.HENTAI)
internal class MangaowlOne(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAOWL_ONE, "mangaowl.one") {
	override val postReq = true
}

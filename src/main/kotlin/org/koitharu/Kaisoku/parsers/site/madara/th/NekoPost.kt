package org.koitharu.Kaisoku.parsers.site.madara.th

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken("Redirect to @KINGS_MANGA")
@MangaSourceParser("NEKOPOST", "NekoPost", "th", ContentType.HENTAI)
internal class NekoPost(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NEKOPOST, "www.superdoujin.org") {
	override val postReq = true
	override val datePattern = "d MMMM yyyy"
}

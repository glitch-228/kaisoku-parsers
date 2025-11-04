package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MILFTOON", "MilfToon", "en", ContentType.HENTAI)
internal class MilfToon(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MILFTOON, "milftoon.xxx", 20) {
	override val postReq = true
	override val datePattern = "d MMMM, yyyy"
}

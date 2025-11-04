package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGASY", "Mangasy", "en")
internal class Mangasy(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGASY, "www.mangasy.com") {
	override val tagPrefix = "manhua-genre/"
}

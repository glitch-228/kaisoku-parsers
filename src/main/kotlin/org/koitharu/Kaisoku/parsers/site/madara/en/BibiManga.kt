package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("BIBIMANGA", "BibiManga", "en", ContentType.HENTAI)
internal class BibiManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.BIBIMANGA, "bibimanga.com") {
	override val datePattern = "MMMM dd, yyyy"
}

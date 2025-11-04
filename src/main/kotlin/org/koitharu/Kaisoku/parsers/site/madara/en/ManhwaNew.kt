package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANHWANEW", "ManhwaNew", "en")
internal class ManhwaNew(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHWANEW, "manhwanew.com") {
	override val datePattern = "MM/dd/yyyy"
}

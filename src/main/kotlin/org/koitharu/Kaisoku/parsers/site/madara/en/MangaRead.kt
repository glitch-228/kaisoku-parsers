package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAREAD", "MangaRead", "en")
internal class MangaRead(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAREAD, "www.mangaread.org") {
	override val tagPrefix = "genres/"
	override val datePattern = "dd.MM.yyyy"
}

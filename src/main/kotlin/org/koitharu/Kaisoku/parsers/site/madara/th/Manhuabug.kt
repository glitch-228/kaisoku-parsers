package org.koitharu.Kaisoku.parsers.site.madara.th

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import java.util.*

@MangaSourceParser("MANHUABUG", "ManhuaBug", "th")
internal class Manhuabug(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHUABUG, "www.manhuabug.com", 10) {
	override val datePattern: String = "d MMMM yyyy"
	override val sourceLocale: Locale = Locale.ENGLISH
	override val selectPage = "img"
}

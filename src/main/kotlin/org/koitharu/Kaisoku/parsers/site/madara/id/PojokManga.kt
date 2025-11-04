package org.koitharu.Kaisoku.parsers.site.madara.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import java.util.*

@MangaSourceParser("POJOKMANGA", "PojokManga", "id")
internal class PojokManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.POJOKMANGA, "pojokmanga.info") {
	override val tagPrefix = "komik-genre/"
	override val listUrl = "komik/"
	override val datePattern = "MMM d, yyyy"
	override val sourceLocale: Locale = Locale.ENGLISH
}

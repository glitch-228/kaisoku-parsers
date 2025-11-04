package org.koitharu.Kaisoku.parsers.site.mmrcms.es

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mmrcms.MmrcmsParser
import java.util.*

@Broken
@MangaSourceParser("MANGADOOR", "MangaDoor", "es")
internal class MangaDoor(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.MANGADOOR, "mangadoor.com") {
	override val sourceLocale: Locale = Locale.ENGLISH
	override val selectState = "dt:contains(Estado)"
	override val selectAlt = "dt:contains(Otros nombres)"
	override val selectAut = "dt:contains(Autor(es))"
	override val selectTag = "dt:contains(Categorías)"
}

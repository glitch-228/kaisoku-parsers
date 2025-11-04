package org.koitharu.Kaisoku.parsers.site.mmrcms.id

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mmrcms.MmrcmsParser
import java.util.*

@Broken
@MangaSourceParser("KOMIKID", "KomikId", "id")
internal class KomikId(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.KOMIKID, "komiku.org") {
	override val selectState = "dt:contains(Status)"
	override val selectAlt = "dt:contains(Other names)"
	override val selectAut = "dt:contains(Author(s))"
	override val selectTag = "dt:contains(Categories)"
	override val sourceLocale: Locale = Locale.ENGLISH
}

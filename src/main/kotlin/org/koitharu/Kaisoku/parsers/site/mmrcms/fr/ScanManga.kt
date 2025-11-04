package org.koitharu.Kaisoku.parsers.site.mmrcms.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mmrcms.MmrcmsParser
import java.util.*

@Broken
@MangaSourceParser("SCANMANGA", "ScanManga", "fr")
internal class ScanManga(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.SCANMANGA, "scan-manga.me") {
	override val imgUpdated = ".jpg"
	override val sourceLocale: Locale = Locale.ENGLISH
}

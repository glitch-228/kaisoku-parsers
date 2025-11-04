package org.koitharu.Kaisoku.parsers.site.mmrcms.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mmrcms.MmrcmsParser
import java.util.*

@MangaSourceParser("SCANVF", "ScanVf", "fr")
internal class ScanVf(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.SCANVF, "www.scan-vf.net") {
	override val sourceLocale: Locale = Locale.ENGLISH
}

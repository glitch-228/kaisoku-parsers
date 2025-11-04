package org.koitharu.Kaisoku.parsers.site.mmrcms.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mmrcms.MmrcmsParser
import java.util.*

@Broken
@MangaSourceParser("JPSCANVF", "LireScanVf.com", "fr")
internal class JpScanVf(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.JPSCANVF, "lirescanvf.com") {
	override val sourceLocale: Locale = Locale.ENGLISH
}

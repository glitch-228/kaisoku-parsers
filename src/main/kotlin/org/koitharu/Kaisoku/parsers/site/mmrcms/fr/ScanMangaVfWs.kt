package org.koitharu.Kaisoku.parsers.site.mmrcms.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mmrcms.MmrcmsParser
import java.util.*

@Broken
@MangaSourceParser("SCANMANGAVF_WS", "ScanMangaVf.ws", "fr")
internal class ScanMangaVfWs(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.SCANMANGAVF_WS, "scanmanga-vf.me") {
	override val imgUpdated = ".jpg"
	override val selectTag = "dt:contains(Genres)"
	override val selectAlt = "dt:contains(Appelé aussi)"
	override val sourceLocale: Locale = Locale.ENGLISH
}

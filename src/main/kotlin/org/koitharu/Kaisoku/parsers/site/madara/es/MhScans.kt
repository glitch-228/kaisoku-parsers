package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MHSCANS", "MhScans", "es")
internal class MhScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MHSCANS, "mhscans.mundoalterno.org") {
	override val datePattern = "d 'de' MMMMM 'de' yyyy"
	override val listUrl = "series/"
}

package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("VALKYRIESCAN", "ValkyrieScan", "pt", ContentType.HENTAI)
internal class ValkyrieScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.VALKYRIESCAN, "valkyriescan.com", pageSize = 10) {
	override val datePattern: String = "dd/MM/yyyy"
}

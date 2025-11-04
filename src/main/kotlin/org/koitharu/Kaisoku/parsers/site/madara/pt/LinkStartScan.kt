package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("LINKSTARTSCAN", "LinkStartScan", "pt")
internal class LinkStartScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LINKSTARTSCAN, "www.linkstartscan.xyz") {
	override val datePattern: String = "dd/MM/yyyy"
}

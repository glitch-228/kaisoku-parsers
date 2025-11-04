package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken("Not dead, changed template")
@MangaSourceParser("CERISE_SCANS", "CeriseScans", "pt")
internal class CeriseScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.CERISE_SCANS, "cerise.leitorweb.com") {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}

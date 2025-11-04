package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TATAKAE_SCANS", "TatakaeScans", "pt")
internal class TatakaeScansParser(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TATAKAE_SCANS, "tatakaescan.com", pageSize = 10) {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}

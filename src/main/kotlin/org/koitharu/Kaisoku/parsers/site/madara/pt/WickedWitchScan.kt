package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("WICKEDWITCHSCAN", "WickedWitchScan", "pt")
internal class WickedWitchScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.WICKEDWITCHSCAN, "wicked-witch-scan.com", pageSize = 10) {
	override val postReq = true
}

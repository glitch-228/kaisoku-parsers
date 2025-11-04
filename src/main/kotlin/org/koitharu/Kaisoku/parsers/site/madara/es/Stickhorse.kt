package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken // Host error
@MangaSourceParser("STICKHORSE", "StickHorse", "es")
internal class Stickhorse(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.STICKHORSE, "www.stickhorse.cl") {
	override val postReq = true
}

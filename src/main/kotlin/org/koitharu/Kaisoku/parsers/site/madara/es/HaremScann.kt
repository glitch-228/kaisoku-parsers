package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Not dead, changed template")
@MangaSourceParser("HAREMSCANN", "HaremScann", "es")
internal class HaremScann(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HAREMSCANN, "haremscann.es") {
	override val postReq = true
}

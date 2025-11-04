package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("DAPROB", "Daprob", "es")
internal class Daprob(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.DAPROB, "daprob.com") {
	override val datePattern = "d 'de' MMMMM 'de' yyyy"
}

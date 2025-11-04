package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("INMORALNOFANSUB", "InmoralNoFansub", "es")
internal class InmoralNoFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.INMORALNOFANSUB, "inmoralnofansub.xyz") {
	override val datePattern = "dd/MM/yyyy"
}

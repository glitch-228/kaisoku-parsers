package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGABOB", "MangaBob", "en")
internal class MangaBob(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGABOB, "mangabob.com") {
	override val postReq = true
}

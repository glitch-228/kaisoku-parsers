package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("GOOFFANSUB", "GoofFansub", "pt", ContentType.HENTAI)
internal class GoofFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GOOFFANSUB, "gooffansub.com") {
	override val datePattern: String = "dd/MM/yyyy"
}

package org.koitharu.Kaisoku.parsers.site.madara.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAHUB", "MangaHub", "fr", ContentType.HENTAI)
internal class MangaHub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAHUB, "mangahub.fr") {
	override val datePattern = "d MMMM yyyy"
}

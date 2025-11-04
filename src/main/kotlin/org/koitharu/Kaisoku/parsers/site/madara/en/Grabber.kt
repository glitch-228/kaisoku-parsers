package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("GRABBER", "Grabber", "en", ContentType.COMICS)
internal class Grabber(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GRABBER, "grabber.zone", 20) {
	override val tagPrefix = "type/"
	override val listUrl = "comics/"
	override val datePattern = "dd.MM.yyyy"
}

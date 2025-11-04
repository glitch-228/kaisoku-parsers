package org.koitharu.Kaisoku.parsers.site.madara.ar

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGASPARK", "Manga-Spark", "ar")
internal class Mangaspark(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGASPARK, "manga-spark.com", pageSize = 10) {
	override val postReq = true
	override val datePattern = "d MMMM، yyyy"
}

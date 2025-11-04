package org.koitharu.Kaisoku.parsers.site.madara.vi

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("PINKTEACOMIC", "PinkTeaComic", "vi")
internal class PinkTeaComic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PINKTEACOMIC, "pinkteacomics.com") {
	override val datePattern = "d MMMM, yyyy"
}

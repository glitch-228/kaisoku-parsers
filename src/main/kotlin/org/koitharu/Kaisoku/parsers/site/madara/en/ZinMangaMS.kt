package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("ZINMANGA_MS", "ZinManga.ms", "en")
internal class ZinMangaMS(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ZINMANGA_MS, "zinmanga.ms") {
	override val listUrl = "manga-1/"
	override val tagPrefix = "manga-genre-1/"
}

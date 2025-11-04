package org.koitharu.Kaisoku.parsers.site.foolslide.it

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.foolslide.FoolSlideParser

@MangaSourceParser("READNIFTEAM", "ReadNifTeam", "it")
internal class ReadNifteam(context: MangaLoaderContext) :
	FoolSlideParser(context, MangaParserSource.READNIFTEAM, "read-nifteam.info") {
	override val searchUrl = "slide/search/"
	override val listUrl = "slide/directory/"
}

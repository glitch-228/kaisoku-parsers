package org.koitharu.Kaisoku.parsers.site.foolslide.it

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.foolslide.FoolSlideParser

@MangaSourceParser("RAMAREADER", "RamaReader", "it")
internal class Ramareader(context: MangaLoaderContext) :
	FoolSlideParser(context, MangaParserSource.RAMAREADER, "www.ramareader.it") {
	override val searchUrl = "read/search/"
	override val listUrl = "read/directory/"
}

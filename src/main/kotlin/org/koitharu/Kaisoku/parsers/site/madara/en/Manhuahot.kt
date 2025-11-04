package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHUAHOT", "ManhuaHot", "en")
internal class Manhuahot(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHUAHOT, "manhuahot.com", 10)

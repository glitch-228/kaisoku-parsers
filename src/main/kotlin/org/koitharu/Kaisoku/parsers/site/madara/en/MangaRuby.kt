package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGARUBY", "MangaRuby", "en")
internal class MangaRuby(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGARUBY, "mangaruby.com", 10)

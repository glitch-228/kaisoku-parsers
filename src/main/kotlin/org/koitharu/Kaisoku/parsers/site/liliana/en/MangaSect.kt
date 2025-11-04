package org.koitharu.Kaisoku.parsers.site.liliana.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.liliana.LilianaParser

@MangaSourceParser("MANGASECT", "MangaSect", "en")
internal class MangaSect(context: MangaLoaderContext) :
	LilianaParser(context, MangaParserSource.MANGASECT, "mangasect.net")

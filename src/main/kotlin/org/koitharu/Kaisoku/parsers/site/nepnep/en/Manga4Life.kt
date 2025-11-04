package org.koitharu.Kaisoku.parsers.site.nepnep.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.nepnep.NepnepParser

// site closed in favour of weeb central
@Broken
@MangaSourceParser("MANGA4LIFE", "Manga4Life", "en")
internal class Manga4Life(context: MangaLoaderContext) :
	NepnepParser(context, MangaParserSource.MANGA4LIFE, "manga4life.com")

package org.koitharu.Kaisoku.parsers.site.madara.ar

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGAPEAK", "MangaPeak", "ar")
internal class MangaPeak(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAPEAK, "mangapeak.org")

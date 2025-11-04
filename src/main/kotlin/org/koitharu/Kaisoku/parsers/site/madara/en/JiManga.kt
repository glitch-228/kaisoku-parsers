package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("JIMANGA", "S2Manga.io", "en")
internal class JiManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.JIMANGA, "s2manga.io")

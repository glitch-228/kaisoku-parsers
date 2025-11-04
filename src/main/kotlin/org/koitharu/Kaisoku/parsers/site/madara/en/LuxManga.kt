package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("LUXMANGA", "LuxManga", "en")
internal class LuxManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LUXMANGA, "luxmanga.net")

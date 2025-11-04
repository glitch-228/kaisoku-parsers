package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("KUNMANGA", "KunManga", "en")
internal class KunManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KUNMANGA, "kunmanga.com", 10)

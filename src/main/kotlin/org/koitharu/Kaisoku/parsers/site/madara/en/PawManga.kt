package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("PAWMANGA", "PawManga", "en", ContentType.HENTAI)
internal class PawManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PAWMANGA, "pawmanga.com", 10)

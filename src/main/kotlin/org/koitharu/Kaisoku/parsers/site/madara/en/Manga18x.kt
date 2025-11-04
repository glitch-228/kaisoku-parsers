package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGA18X", "Manga18x", "en", ContentType.HENTAI)
internal class Manga18x(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA18X, "manga18x.net", 24)

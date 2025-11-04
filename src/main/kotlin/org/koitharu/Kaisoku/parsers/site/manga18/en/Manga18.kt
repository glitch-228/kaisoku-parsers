package org.koitharu.Kaisoku.parsers.site.manga18.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.manga18.Manga18Parser

@MangaSourceParser("MANGA18", "Manga18", "en", ContentType.HENTAI)
internal class Manga18(context: MangaLoaderContext) :
	Manga18Parser(context, MangaParserSource.MANGA18, "manga18.club")

package org.koitharu.Kaisoku.parsers.site.likemanga.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.likemanga.LikeMangaParser

@MangaSourceParser("LIKEMANGA", "LikeManga", "en")
internal class LikeManga(context: MangaLoaderContext) :
	LikeMangaParser(context, MangaParserSource.LIKEMANGA, "likemanga.ink")

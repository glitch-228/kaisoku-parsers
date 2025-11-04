package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGA_QUEEN", "MangaQueen", "en")
internal class MangaQueen(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA_QUEEN, "mangaqueen.com", 16)

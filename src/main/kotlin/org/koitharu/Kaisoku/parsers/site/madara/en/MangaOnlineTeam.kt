package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAONLINETEAM", "MangaOnlineTeam", "en")
internal class MangaOnlineTeam(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAONLINETEAM, "mangaonlineteam.com", 10)

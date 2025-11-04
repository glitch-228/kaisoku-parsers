package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGACULTIVATOR", "MangaCultivator", "en")
internal class MangaCultivator(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGACULTIVATOR, "mangacultivator.com", 10)

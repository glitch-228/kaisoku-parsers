package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("ARYASCANS", "AryaScans", "en")
internal class AryaScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ARYASCANS, "aryascans.com")

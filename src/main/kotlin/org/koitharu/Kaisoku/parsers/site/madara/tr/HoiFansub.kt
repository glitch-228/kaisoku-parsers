package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("HOIFANSUB", "HoiFansub", "tr")
internal class HoiFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HOIFANSUB, "hoifansub.com", 20)

package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("ZEVEP", "Zevep", "es")
internal class Zevep(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ZEVEP, "zevep.com", 16)

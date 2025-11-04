package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("BOKUGENTS", "Bokugents", "es")
internal class Bokugents(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.BOKUGENTS, "bokugents.com")
// For this source need to enable the option to ignore SSL errors

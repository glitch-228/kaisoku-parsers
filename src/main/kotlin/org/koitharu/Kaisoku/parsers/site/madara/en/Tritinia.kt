package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TRITINIA", "Tritinia", "en")
internal class Tritinia(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TRITINIA, "tritinia.org")

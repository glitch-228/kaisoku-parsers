package org.koitharu.Kaisoku.parsers.site.onemanga.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.onemanga.OneMangaParser

@MangaSourceParser("OSHINOKO", "OshiNoKo", "fr")
internal class OshiNoKo(context: MangaLoaderContext) :
	OneMangaParser(context, MangaParserSource.OSHINOKO, "oshinoko.fr")

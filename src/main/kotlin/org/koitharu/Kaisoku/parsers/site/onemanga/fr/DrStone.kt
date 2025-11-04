package org.koitharu.Kaisoku.parsers.site.onemanga.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.onemanga.OneMangaParser

@MangaSourceParser("DRSTONE", "DrStone", "fr")
internal class DrStone(context: MangaLoaderContext) :
	OneMangaParser(context, MangaParserSource.DRSTONE, "drstone.fr")

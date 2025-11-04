package org.koitharu.Kaisoku.parsers.site.onemanga.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.onemanga.OneMangaParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("CENTURYBOYS20TH", "20ThCenturyBoys", "fr")
internal class CenturyBoys20Th(context: MangaLoaderContext) :
	OneMangaParser(context, MangaParserSource.CENTURYBOYS20TH, "20thcenturyboys.fr")

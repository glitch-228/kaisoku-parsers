package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("ARCANESCANS", "ArcaneScans", "en")
internal class ArcaneScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ARCANESCANS, "arcanescans.com", 10)

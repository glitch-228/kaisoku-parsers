package org.koitharu.Kaisoku.parsers.site.iken.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.iken.IkenParser

@Broken("Redirect to VortexScans")
@MangaSourceParser("MANGAGALAXY", "MangaGalaxy", "en")
internal class MangaGalaxyParser(context: MangaLoaderContext) :
	IkenParser(context, MangaParserSource.MANGAGALAXY, "vortexscans.org", 18)

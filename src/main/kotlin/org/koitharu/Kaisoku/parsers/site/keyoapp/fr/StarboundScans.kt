package org.koitharu.Kaisoku.parsers.site.keyoapp.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.keyoapp.KeyoappParser

@Broken
@MangaSourceParser("STARBOUNDSCANS", "StarboundScans", "fr")
internal class StarboundScans(context: MangaLoaderContext) :
	KeyoappParser(context, MangaParserSource.STARBOUNDSCANS, "starboundscans.com")

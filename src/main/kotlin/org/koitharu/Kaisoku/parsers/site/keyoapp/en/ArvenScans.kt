package org.koitharu.Kaisoku.parsers.site.keyoapp.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.keyoapp.KeyoappParser

@MangaSourceParser("ARVENSCANS", "ArvenComics", "en")
internal class ArvenScans(context: MangaLoaderContext) :
	KeyoappParser(context, MangaParserSource.ARVENSCANS, "arvencomics.com")

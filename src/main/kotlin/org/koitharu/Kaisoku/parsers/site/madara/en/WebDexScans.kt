package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("WEBDEXSCANS", "WebDexScans", "en")
internal class WebDexScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.WEBDEXSCANS, "webdexscans.com")

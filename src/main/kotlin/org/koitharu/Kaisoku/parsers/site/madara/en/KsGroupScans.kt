package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("KSGROUPSCANS", "KsGroupScans", "en")
internal class KsGroupScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KSGROUPSCANS, "ksgroupscans.com")

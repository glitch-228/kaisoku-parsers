package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAWT_NET", "MangaWt.net", "tr")
internal class MangaWtNet(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAWT_NET, "mangawt.com")

package org.koitharu.Kaisoku.parsers.site.sinmh.zh

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.sinmh.SinmhParser

@MangaSourceParser("YKMH", "Ykmh", "zh")
internal class Ykmh(context: MangaLoaderContext) :
	SinmhParser(context, MangaParserSource.YKMH, "www.ykmh.net")

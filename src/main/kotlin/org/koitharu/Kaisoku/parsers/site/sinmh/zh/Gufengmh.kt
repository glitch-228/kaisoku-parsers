package org.koitharu.Kaisoku.parsers.site.sinmh.zh

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.sinmh.SinmhParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("GUFENGMH", "Gufengmh", "zh")
internal class Gufengmh(context: MangaLoaderContext) :
	SinmhParser(context, MangaParserSource.GUFENGMH, "www.gufengmh.com")

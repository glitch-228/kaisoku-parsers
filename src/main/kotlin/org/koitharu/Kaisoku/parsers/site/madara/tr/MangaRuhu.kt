package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGARUHU", "MangaRuhu", "tr")
internal class MangaRuhu(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGARUHU, "mangaruhu.com", 16)

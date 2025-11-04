package org.koitharu.Kaisoku.parsers.site.madara.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGALIONZ", "Manga-Lionz", "ar")
internal class MangaLionz(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGALIONZ, "manga-lionz.com", pageSize = 10)

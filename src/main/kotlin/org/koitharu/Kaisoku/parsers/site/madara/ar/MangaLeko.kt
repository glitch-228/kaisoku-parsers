package org.koitharu.Kaisoku.parsers.site.madara.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGALEKO", "Manga-Leko.org", "ar")
internal class MangaLeko(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGALEKO, "manga-leko.org", pageSize = 10)

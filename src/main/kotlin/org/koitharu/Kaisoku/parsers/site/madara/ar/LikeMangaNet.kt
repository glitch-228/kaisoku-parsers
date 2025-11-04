package org.koitharu.Kaisoku.parsers.site.madara.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("LIKEMANGANET", "Like-Manga.net", "ar")
internal class LikeMangaNet(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LIKEMANGANET, "like-manga.net", pageSize = 10)

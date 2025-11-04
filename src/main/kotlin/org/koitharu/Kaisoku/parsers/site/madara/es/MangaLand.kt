package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGALAND", "MangaLand", "es", ContentType.HENTAI)
internal class MangaLand(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGALAND, "mangaland.net")

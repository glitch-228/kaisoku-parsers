package org.koitharu.Kaisoku.parsers.site.mangareader.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("TRADUCCIONESMOONLIGHT", "TraduccionesMoonlight", "es", ContentType.HENTAI)
internal class TraduccionesMoonlight(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.TRADUCCIONESMOONLIGHT, "traduccionesmoonlight.com", 20, 10)

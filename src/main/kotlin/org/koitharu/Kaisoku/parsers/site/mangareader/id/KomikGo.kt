package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("KOMIKGO", "KomikGo", "id", ContentType.HENTAI)
internal class KomikGo(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOMIKGO, "komikgo.xyz", pageSize = 20, searchPageSize = 10)

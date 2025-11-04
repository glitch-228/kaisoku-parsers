package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("KOMIKMAMA", "KomikMama", "id")
internal class KomikMama(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOMIKMAMA, "komikmama.lat", pageSize = 30, searchPageSize = 10) {
	override val listUrl = "/komik"
}

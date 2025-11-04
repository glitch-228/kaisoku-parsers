package org.koitharu.Kaisoku.parsers.site.mangareader.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("CULTUREDWORKS", "CulturedWorks", "en")
internal class CulturedWorks(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.CULTUREDWORKS,
		"culturedworks.com",
		pageSize = 20,
		searchPageSize = 10,
	) {
	override val selectMangaList = ".listupd .bs .bsx"
}

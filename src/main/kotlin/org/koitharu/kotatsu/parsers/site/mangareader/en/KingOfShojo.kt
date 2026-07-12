package org.koitharu.kotatsu.parsers.site.mangareader.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("KINGOFSHOJO", "King of Shojo", "en")
internal class KingOfShojo(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.KINGOFSHOJO,
		"kingofshojo.com",
		pageSize = 20,
		searchPageSize = 20,
	) {
	override val selectMangaList = ".listupd .bsx"
}

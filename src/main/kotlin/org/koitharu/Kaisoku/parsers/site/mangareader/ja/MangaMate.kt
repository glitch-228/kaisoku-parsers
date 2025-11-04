package org.koitharu.Kaisoku.parsers.site.mangareader.ja

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import java.util.*

@MangaSourceParser("MANGAMATE", "MangaMate", "ja")
internal class MangaMate(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGAMATE, "manga-mate.org", pageSize = 10, searchPageSize = 10) {
	override val datePattern = "M月 d, yyyy"
	override val sourceLocale: Locale = Locale.ENGLISH
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

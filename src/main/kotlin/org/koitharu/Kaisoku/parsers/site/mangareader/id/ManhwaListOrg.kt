package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import java.util.Locale

@MangaSourceParser("MANHWALIST_ORG", "ManhwaList.org", "id")
internal class ManhwaListOrg(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.MANHWALIST_ORG,
		"isekaikomik.com",
		pageSize = 30,
		searchPageSize = 10,
	) {
	override val sourceLocale: Locale = Locale.ENGLISH
	override val listUrl = "/manga"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

package org.koitharu.Kaisoku.parsers.site.mangareader.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaListFilterCapabilities
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser
import java.util.*

@MangaSourceParser("KOMIKDEWASA", "komikRemaja.icu", "id", ContentType.HENTAI)
internal class KomikDewasaParser(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOMIKDEWASA, "komikremaja.icu", pageSize = 20, searchPageSize = 10) {
	override val listUrl: String = "/komik"
	override val sourceLocale: Locale = Locale.ENGLISH
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

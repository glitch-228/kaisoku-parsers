package org.koitharu.Kaisoku.parsers.site.hotcomics.de

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.hotcomics.HotComicsParser
import java.util.Locale

@Broken
@MangaSourceParser("TOOMICS", "Toomics", "de")
internal class Toomics(context: MangaLoaderContext) :
	HotComicsParser(context, MangaParserSource.TOOMICS, "toomics.top/de") {
	override val sourceLocale: Locale = Locale.ENGLISH
	override val isSearchSupported = false
}

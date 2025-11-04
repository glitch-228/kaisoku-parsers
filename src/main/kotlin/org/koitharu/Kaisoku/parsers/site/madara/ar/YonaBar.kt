package org.koitharu.Kaisoku.parsers.site.madara.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import java.util.Locale

@MangaSourceParser("YONABAR", "YonaBar", "ar")
internal class YonaBar(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YONABAR, "yonabar.xyz", 10) {
	override val sourceLocale: Locale = Locale.ENGLISH
	override val listUrl = "yaoi/"
}

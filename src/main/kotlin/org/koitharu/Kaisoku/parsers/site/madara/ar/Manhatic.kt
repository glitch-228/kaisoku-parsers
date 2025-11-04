package org.koitharu.Kaisoku.parsers.site.madara.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import java.util.*

@MangaSourceParser("MANHATIC", "Manhatic", "ar", ContentType.HENTAI)
internal class Manhatic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHATIC, "manhatic.com") {
	override val sourceLocale: Locale = Locale.ENGLISH
}

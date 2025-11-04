package org.koitharu.Kaisoku.parsers.site.madara.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("KLIKMANGA", "KlikManga", "id", ContentType.HENTAI)
internal class KlikManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KLIKMANGA, "klikmanga.com", 36) {
	override val tagPrefix = "genre/"
	override val datePattern = "MMM d, yyyy"
}

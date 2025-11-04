package org.koitharu.Kaisoku.parsers.site.madara.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("COMICARAB", "ComicArab", "ar")
internal class ComicArab(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.COMICARAB, "comicarab.com", pageSize = 24) {
	override val datePattern = "d MMMM، yyyy"
}

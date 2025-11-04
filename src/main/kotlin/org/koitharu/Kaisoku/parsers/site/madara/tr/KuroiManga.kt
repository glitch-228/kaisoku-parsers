package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("KUROIMANGA", "KuroiManga", "tr", ContentType.HENTAI)
internal class KuroiManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KUROIMANGA, "www.kuroimanga.com") {
	override val datePattern = "d MMMM yyyy"
}

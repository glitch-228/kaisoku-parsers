package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("ALLIED_FANSUB", "AlliedFansub", "tr", ContentType.HENTAI)
internal class AlliedFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ALLIED_FANSUB, "alliedfansub.net", 20) {
	override val datePattern = "dd/MM/yyyy"
}
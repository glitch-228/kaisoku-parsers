package org.koitharu.Kaisoku.parsers.site.madara.id

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("INDO18H", "Indo18h", "id", ContentType.HENTAI)
internal class Indo18h(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.INDO18H, "indo18h.com", 24) {
	override val withoutAjax = true
}

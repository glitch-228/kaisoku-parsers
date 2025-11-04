package org.koitharu.Kaisoku.parsers.site.madara.vi

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("TRUYENVN", "TruyenVn", "vi", ContentType.HENTAI)
internal class TruyenVn(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TRUYENVN, "truyenvn.shop", 20) {
	override val listUrl = "truyen-tranh/"
	override val tagPrefix = "the-loai/"
	override val datePattern = "dd/MM/yyyy"
}

package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHUAZONGHE", "ManhuaZonghe", "en")
internal class ManhuaZonghe(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHUAZONGHE, "www.manhuazonghe.com") {
	override val tagPrefix = "genre/"
	override val listUrl = "manhua/"
}

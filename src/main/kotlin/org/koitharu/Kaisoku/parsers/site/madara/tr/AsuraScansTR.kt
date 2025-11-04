package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("ASURASCANSTR", "AsuraScansTR", "tr")
internal class AsuraScansTR(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ASURASCANSTR, "asurascans.com.tr") {
	override val tagPrefix = "tur/"
}

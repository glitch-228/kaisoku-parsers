package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("GOURMETSCANS", "GourmetScans", "en")
internal class GourmetScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GOURMETSCANS, "gourmetsupremacy.com") {
	override val listUrl = "project/"
	override val tagPrefix = "genre/"
	override val stylePage = ""
}

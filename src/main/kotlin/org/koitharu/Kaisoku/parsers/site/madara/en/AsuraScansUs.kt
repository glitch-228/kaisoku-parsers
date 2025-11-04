package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("ASURASCANS_US", "AsuraScans.us", "en")
internal class AsuraScansUs(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ASURASCANS_US, "asurascans.us") {
	override val listUrl = "comics/"
	override val tagPrefix = "read-en-us-genre/"
}

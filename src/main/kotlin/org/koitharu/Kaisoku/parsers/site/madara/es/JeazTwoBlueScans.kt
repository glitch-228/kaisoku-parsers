package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("JEAZTWOBLUESCANS", "Lector HUB", "es")
internal class JeazTwoBlueScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.JEAZTWOBLUESCANS, "lectorhub.j5z.xyz") {
	override val datePattern = "d MMMM, yyyy"
}

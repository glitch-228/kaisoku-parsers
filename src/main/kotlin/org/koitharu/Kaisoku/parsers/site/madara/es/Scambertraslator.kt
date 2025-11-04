package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("SCAMBERTRASLATOR", "ScamberTraslator", "es")
internal class Scambertraslator(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.SCAMBERTRASLATOR, "scambertraslator.com") {
	override val datePattern = "dd/MM/yyyy"
}

package org.koitharu.Kaisoku.parsers.site.madara.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.*
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken("Blocked by Cloudflare")
@MangaSourceParser("EPSILONSOFT", "EpsilonSoft", "fr")
internal class EpsilonSoft(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.EPSILONSOFT, "epsilonsoft.to") {
	override val datePattern = "dd/MM/yy"
	override val withoutAjax = true
}

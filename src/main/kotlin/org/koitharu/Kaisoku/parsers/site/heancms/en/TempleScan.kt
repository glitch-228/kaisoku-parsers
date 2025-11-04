package org.koitharu.Kaisoku.parsers.site.heancms.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.heancms.HeanCms

@Broken("Not dead, changed template")
@MangaSourceParser("TEMPLESCAN", "TempleScan", "en")
internal class TempleScan(context: MangaLoaderContext) :
	HeanCms(context, MangaParserSource.TEMPLESCAN, "templetoons.com") {
	override val pathManga = "comic"

	override val apiPath: String
		get() = "$domain/api"
}

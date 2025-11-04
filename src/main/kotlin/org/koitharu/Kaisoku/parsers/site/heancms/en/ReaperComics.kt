package org.koitharu.Kaisoku.parsers.site.heancms.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.*
import org.koitharu.Kaisoku.parsers.site.heancms.HeanCms
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Closed site")
@MangaSourceParser("REAPERCOMICS", "ReaperComics", "en")
internal class ReaperComics(context: MangaLoaderContext) :
	HeanCms(context, MangaParserSource.REAPERCOMICS, "reaperscans.com") {
	override val cdn = "media.reaperscans.com/file/4SRBHm//"
	override val paramsUpdated = "updated_at"
	override val selectPages = ".flex > img"

	override fun reqUrl(seriesId: Long): String {
		return "https://$apiPath/chapters/$seriesId?page=1&perPage=9999&order=desc"
	}
}

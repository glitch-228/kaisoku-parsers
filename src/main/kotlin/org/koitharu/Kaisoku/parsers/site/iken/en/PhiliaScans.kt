package org.koitharu.Kaisoku.parsers.site.iken.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.iken.IkenParser
import org.koitharu.Kaisoku.parsers.model.*
import org.koitharu.Kaisoku.parsers.util.*

@MangaSourceParser("PHILIASCANS", "PhiliaScans", "en")
internal class PhiliaScans(context: MangaLoaderContext) :
	IkenParser(context, MangaParserSource.PHILIASCANS, "philiascans.org", 18) {

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(fullUrl).parseHtml()
		return doc.select(selectPages).map { img ->
			val url = img.requireSrc()
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}
}
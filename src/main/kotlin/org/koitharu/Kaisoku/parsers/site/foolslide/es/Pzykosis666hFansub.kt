package org.koitharu.Kaisoku.parsers.site.foolslide.es

import kotlinx.coroutines.coroutineScope
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.Manga
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.foolslide.FoolSlideParser
import org.koitharu.Kaisoku.parsers.util.*

@MangaSourceParser("PZYKOSIS666HFANSUB", "Pzykosis666h Fansub", "es", ContentType.HENTAI)
internal class Pzykosis666hFansub(context: MangaLoaderContext) :
	FoolSlideParser(context, MangaParserSource.PZYKOSIS666HFANSUB, "lector.pzykosis666hfansub.com") {

	override suspend fun getDetails(manga: Manga): Manga = coroutineScope {
		val fullUrl = manga.url.toAbsoluteUrl(domain)
		val testAdultPage = webClient.httpGet(fullUrl).parseHtml()
		val doc = if (testAdultPage.selectFirst("div.info form") != null) {
			webClient.httpPost(fullUrl, "adult=true").parseHtml()
		} else {
			testAdultPage
		}
		val chapters = getChapters(doc)
		val desc = if (doc.selectFirst(selectInfo)?.html()?.contains("Descripción") == true) {
			doc.selectFirst(selectInfo)?.text()?.substringAfter("Descripción: ")?.substringBefore("Lecturas")
		} else {
			doc.selectFirst(selectInfo)?.text()
		}
		val author = if (doc.selectFirst(selectInfo)?.html()?.contains("Author") == true) {
			doc.selectFirst(selectInfo)?.text()?.substringAfter("Author: ")?.substringBefore("Art")
		} else {
			null
		}
		manga.copy(
			coverUrl = doc.selectFirst(".thumbnail img")?.src() ?: manga.coverUrl,
			description = desc?.nullIfEmpty(),
			authors = author?.nullIfEmpty()?.let { setOf(it) } ?: emptySet(),
			chapters = chapters,
		)
	}
}

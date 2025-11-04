package org.koitharu.Kaisoku.parsers.site.manga18.zh

import org.jsoup.nodes.Document
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaChapter
import org.koitharu.Kaisoku.parsers.model.MangaListFilterOptions
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.manga18.Manga18Parser
import org.koitharu.Kaisoku.parsers.util.attrAsRelativeUrl
import org.koitharu.Kaisoku.parsers.util.generateUid
import org.koitharu.Kaisoku.parsers.util.mapChapters
import org.koitharu.Kaisoku.parsers.util.selectFirstOrThrow

@MangaSourceParser("HANMAN18", "Hanman18", "zh", ContentType.HENTAI)
internal class Hanman18(context: MangaLoaderContext) :
	Manga18Parser(context, MangaParserSource.HANMAN18, "hanman18.com") {

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = emptySet(),
	)

	override suspend fun getChapters(doc: Document): List<MangaChapter> {
		return doc.body().select(selectChapter).mapChapters(reversed = true) { i, li ->
			val a = li.selectFirstOrThrow("a")
			val href = a.attrAsRelativeUrl("href")
			MangaChapter(
				id = generateUid(href),
				title = a.text(),
				number = i + 1f,
				volume = 0,
				url = href,
				uploadDate = 0,
				source = source,
				scanlator = null,
				branch = null,
			)
		}
	}
}

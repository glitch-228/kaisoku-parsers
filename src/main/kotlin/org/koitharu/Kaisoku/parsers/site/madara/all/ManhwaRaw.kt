package org.koitharu.Kaisoku.parsers.site.madara.all

import org.jsoup.nodes.Document
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.Manga
import org.koitharu.Kaisoku.parsers.model.MangaChapter
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.util.attrAsRelativeUrlOrNull
import org.koitharu.Kaisoku.parsers.util.generateUid
import org.koitharu.Kaisoku.parsers.util.mapChapters
import org.koitharu.Kaisoku.parsers.util.parseFailed
import java.text.SimpleDateFormat

@MangaSourceParser("MANHWARAW", "ManhwaRaw", "", ContentType.HENTAI)
internal class ManhwaRaw(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHWARAW, "manhwa-raw.com", 10) {
	override val datePattern = "MM/dd"
	override suspend fun getChapters(manga: Manga, doc: Document): List<MangaChapter> {
		val dateFormat = SimpleDateFormat(datePattern, sourceLocale)
		return doc.body().select(selectChapter).mapChapters(reversed = true) { i, li ->
			val a = li.selectFirst("a")
			val href = a?.attrAsRelativeUrlOrNull("href") ?: li.parseFailed("Link is missing")
			val link = href + stylePage
			val dateText = li.selectFirst("a.c-new-tag")?.attr("title") ?: li.selectFirst(selectDate)?.text()
			val name = a.selectFirst("h4")?.text() ?: a.ownText()
			MangaChapter(
				id = generateUid(href),
				title = name,
				number = i + 1f,
				volume = 0,
				url = link,
				uploadDate = if (dateText == "Newly Published!") {
					parseChapterDate(
						dateFormat,
						"today",
					)
				} else {
					parseChapterDate(
						dateFormat,
						dateText,
					)
				},
				source = source,
				scanlator = null,
				branch = null,
			)
		}
	}
}

package org.koitharu.Kaisoku.parsers.site.zmanga.id

import org.jsoup.nodes.Document
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaChapter
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zmanga.ZMangaParser
import org.koitharu.Kaisoku.parsers.util.attrAsRelativeUrl
import org.koitharu.Kaisoku.parsers.util.generateUid
import org.koitharu.Kaisoku.parsers.util.mapChapters
import org.koitharu.Kaisoku.parsers.util.selectFirstOrThrow
import java.text.SimpleDateFormat

// Info: Some scans are password-protected
@MangaSourceParser("MAID_ID", "MaidId", "id")
internal class MaidId(context: MangaLoaderContext) :
	ZMangaParser(context, MangaParserSource.MAID_ID, "www.maid.my.id") {

	override suspend fun getChapters(doc: Document): List<MangaChapter> {
		val dateFormat = SimpleDateFormat(datePattern, sourceLocale)
		return doc.body().select(selectChapter).mapChapters(reversed = true) { i, li ->
			val a = li.selectFirstOrThrow("a")
			val href = a.attrAsRelativeUrl("href")
			val dateText = li.selectFirst(selectDate)?.text()
			val numChapter = li.selectFirstOrThrow(".flexch-infoz span").html().substringAfterLast("Chapter ")
				.substringBefore("<span")
			MangaChapter(
				id = generateUid(href),
				title = null,
				number = i + 1f,
				volume = 0,
				url = href,
				uploadDate = parseChapterDate(
					dateFormat,
					dateText,
				),
				source = source,
				scanlator = null,
				branch = null,
			)
		}
	}
}

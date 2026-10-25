package org.koitharu.kotatsu.parsers.site.mangareader.es

import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser
import org.koitharu.kotatsu.parsers.util.*
import java.text.SimpleDateFormat

@MangaSourceParser("MIAUSCAN", "LectorMiau", "es")
internal class MiauScan(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MIAUSCAN, "leemiau.com", pageSize = 20, searchPageSize = 10) {

	override val selectMangaList = ".listupd .bs .bsx"

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val current = doc.select(".lm4-chapter-grid .lm4-chapter-link[href]")
		if (current.isEmpty()) {
			val chapters = doc.select(selectChapter).mapNotNull { element ->
				val url = element.selectFirst("a")?.attrAsRelativeUrlOrNull("href")
					?.takeUnless { it.contains("{{") || it.startsWith("#") } ?: return@mapNotNull null
				val name = element.selectFirst(".chapternum")?.textOrNull().orEmpty()
				MangaChapter(
					id = generateUid(url), title = name, number = name.extractChapterNumber(), volume = 0,
					url = url, scanlator = null, uploadDate = parseChapterDate(element.selectFirst(".chapterdate")?.text()),
					branch = null, source = source,
				)
			}.sortedBy { it.number }
			return parseInfo(doc, manga, chapters)
		}
		val dateFormat = SimpleDateFormat("dd/MM/yyyy", sourceLocale)
		val chapters = current.mapNotNull { link ->
			val url = link.attrAsRelativeUrlOrNull("href")
				?.takeUnless { it.contains("{{") || it.startsWith("#") } ?: return@mapNotNull null
			val number = link.attr("data-chapter-number").toFloatOrNull()?.takeIf { it.isFinite() }
				?: return@mapNotNull null
			MangaChapter(
				id = generateUid(url),
				title = link.attrOrNull("data-chapter-title") ?: link.selectFirst(".lm4-chapter-name")?.text(),
				number = number,
				volume = 0,
				url = url,
				scanlator = null,
				uploadDate = dateFormat.parseSafe(link.selectFirst(".lm4-chapter-date")?.text()),
				branch = null,
				source = source,
			)
		}.distinctBy { it.url }.sortedBy { it.number }
		return parseInfo(doc, manga, chapters).let { details ->
			details.copy(description = doc.selectFirst(".lm4-summary .entry-content, .lm4-summary .lm4-synopsis")
				?.textOrNull() ?: details.description ?: manga.description)
		}
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		val rendered = doc.select("#readerarea img").mapNotNull { image ->
			image.src()?.takeUnless { it.startsWith("data:") }
		}
		val images = rendered.ifEmpty {
			val script = doc.selectFirstOrThrow(selectTestScript).data()
			val data = JSONObject(script.substringAfter("ts_reader.run(").substringBeforeLast(')'))
			val array = data.getJSONArray("sources").getJSONObject(0).getJSONArray("images")
			List(array.length()) { array.getString(it) }
		}
		return images.map { image ->
			val url = image.toRelativeUrl(domain)
			MangaPage(id = generateUid(url), url = url, preview = null, source = source)
		}
	}
}

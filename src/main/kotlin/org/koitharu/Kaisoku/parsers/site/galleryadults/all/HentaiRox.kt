package org.koitharu.Kaisoku.parsers.site.galleryadults.all

import org.jsoup.nodes.Element
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaListFilterOptions
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.model.MangaTag
import org.koitharu.Kaisoku.parsers.site.galleryadults.GalleryAdultsParser
import org.koitharu.Kaisoku.parsers.util.mapToSet
import org.koitharu.Kaisoku.parsers.util.removeSuffix
import java.util.*

@MangaSourceParser("HENTAIROX", "HentaiRox", type = ContentType.HENTAI)
internal class HentaiRox(context: MangaLoaderContext) :
	GalleryAdultsParser(context, MangaParserSource.HENTAIROX, "hentairox.com") {
	override val selectGalleryImg = ".inner_thumb img"
	override val selectTags = ".gtags"
	override val selectTag = "li:contains(Tags:)"
	override val selectAuthor = "li:contains(Artists:) span.item_name"
	override val selectLanguageChapter = "li:contains(Languages:) .item_name"

	override suspend fun getFilterOptions(): MangaListFilterOptions {
		return super.getFilterOptions().copy(
			availableLocales = setOf(
				Locale.ENGLISH,
				Locale.FRENCH,
				Locale.JAPANESE,
				Locale("es"),
				Locale("ru"),
				Locale("ko"),
				Locale.GERMAN,
			),
		)
	}

	override fun Element.parseTags() = select("a.tag, .gallery_title a").mapToSet {
		val key = it.attr("href").removeSuffix('/').substringAfterLast('/')
		val name = it.selectFirst(".item_name")?.text() ?: it.text()
		MangaTag(
			key = key,
			title = name,
			source = source,
		)
	}
}

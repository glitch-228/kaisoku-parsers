package org.koitharu.Kaisoku.parsers.site.mangabox.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.config.ConfigKey
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangabox.MangaboxParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("MANGANATO", "Manganato", "en")
internal class Manganato(context: MangaLoaderContext) :
	MangaboxParser(context, MangaParserSource.MANGANATO) {
	override val configKeyDomain = ConfigKey.Domain(
		"natomanga.com",
		"manganato.gg",
	)
	override val otherDomain = "manganato.gg"

	override val authorUrl = "/author/story"
	override val selectPage = ".container-chapter-reader > img"
}

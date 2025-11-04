package org.koitharu.Kaisoku.parsers.site.foolslide.it

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.foolslide.FoolSlideParser

@Broken
@MangaSourceParser("POWERMANGA", "PowerManga", "it")
internal class PowerManga(context: MangaLoaderContext) :
	FoolSlideParser(context, MangaParserSource.POWERMANGA, "reader.powermanga.org") {
	override val pagination = false
}

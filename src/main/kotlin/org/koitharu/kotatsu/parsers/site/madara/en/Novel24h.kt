package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("TWENTYFOURHNOVEL", "24HNovel", "en")
internal class Novel24h(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TWENTYFOURHNOVEL, "24hnovel.com") {

	override val withoutAjax = true
}

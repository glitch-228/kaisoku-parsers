package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHWANEX", "ManhwaNex", "en", ContentType.MANHWA)
internal class ManhwaNex(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHWANEX, "manhwanex.com") {

	override val withoutAjax = true
}

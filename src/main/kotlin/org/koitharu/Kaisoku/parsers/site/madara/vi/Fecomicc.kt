package org.koitharu.Kaisoku.parsers.site.madara.vi

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken("No longer available")
@MangaSourceParser("FECOMICC", "Fecomic", "vi")
internal class Fecomicc(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.FECOMICC, "mangasup.net", 9) {
	override val listUrl = "comic/"
	override val tagPrefix = "the-loai/"
	override val datePattern = "dd/MM/yyyy"
}

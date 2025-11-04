package org.koitharu.Kaisoku.parsers.site.madara.id

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("WORLDMANHWAS", "WorldManhwas", "id", ContentType.HENTAI)
internal class WorldManhwas(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.WORLDMANHWAS, "worldmanhwas.zone", 10) {
	override val tagPrefix = "genre/"
	override val listUrl = "komik/"
	override val datePattern = "d MMMM yyyy"
}

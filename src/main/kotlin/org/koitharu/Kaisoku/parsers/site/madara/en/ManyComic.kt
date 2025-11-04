package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("MANYCOMIC", "ManyComic", "en", ContentType.HENTAI)
internal class ManyComic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANYCOMIC, "manycomic.com") {
	override val postReq = true
	override val tagPrefix = "comic-genre/"
}

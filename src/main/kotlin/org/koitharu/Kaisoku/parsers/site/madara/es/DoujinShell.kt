package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("DOUJINSHELL", "DoujinShell", "es", ContentType.HENTAI)
internal class DoujinShell(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.DOUJINSHELL, "www.doujinshell.com", 10) {
	override val datePattern = "dd MMMM, yyyy"
	override val listUrl = "doujin/"
	override val tagPrefix = "doujin-genero/"
	override val selectPage = "img:not(.aligncenter)"
}

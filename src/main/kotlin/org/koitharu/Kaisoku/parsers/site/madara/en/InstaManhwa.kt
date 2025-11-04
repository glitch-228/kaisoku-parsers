package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.*
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import java.util.*

@Broken("Redirect to @XMANHWA")
@MangaSourceParser("INSTAMANHWA", "InstaManhwa", "en", ContentType.HENTAI)
internal class InstaManhwa(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.INSTAMANHWA, "www.manhwaden.com", 15) {
	override val sourceLocale: Locale = Locale.ENGLISH
	override val selectPage = "img"
}

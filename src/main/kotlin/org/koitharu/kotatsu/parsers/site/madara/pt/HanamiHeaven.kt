package org.koitharu.kotatsu.parsers.site.madara.pt

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser
import java.util.Locale

@MangaSourceParser("HANAMIHEAVEN", "HanamiHeaven", "pt", ContentType.HENTAI)
internal class HanamiHeaven(context: MangaLoaderContext) :
    MadaraParser(context, MangaParserSource.HANAMIHEAVEN, "hanamiheaven.org") {

    override val sourceLocale: Locale = Locale.forLanguageTag("pt-BR")
    override val datePattern = "dd/MM/yyyy"
    override val postReq = false
}

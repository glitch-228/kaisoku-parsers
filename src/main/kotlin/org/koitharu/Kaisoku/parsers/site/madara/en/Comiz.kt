package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("COMIZ", "Comiz", "en", ContentType.HENTAI)
internal class Comiz(context: MangaLoaderContext) : MadaraParser(context, MangaParserSource.COMIZ, "v2.comiz.net", 10)

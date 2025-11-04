package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken("Redirect to @hentai20")
@MangaSourceParser("MANGA18H", "Manga18h", "en", ContentType.HENTAI)
internal class Manga18h(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA18H, "manga18h.xyz", 20)

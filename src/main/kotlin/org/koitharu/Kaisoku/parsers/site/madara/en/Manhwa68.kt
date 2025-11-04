package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHWA68", "Manhwa68", "en", ContentType.HENTAI)
internal class Manhwa68(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHWA68, "manhwa68.com")

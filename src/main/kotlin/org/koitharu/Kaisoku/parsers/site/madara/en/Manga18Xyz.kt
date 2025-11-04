package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGA18XYZ", "Manga18.xyz", "en", ContentType.HENTAI)
internal class Manga18Xyz(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA18XYZ, "manga18.xyz", 36)

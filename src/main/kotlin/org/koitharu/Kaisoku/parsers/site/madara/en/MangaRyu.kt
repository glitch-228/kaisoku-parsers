package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGARYU", "MangaRyu", "en", ContentType.HENTAI)
internal class MangaRyu(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGARYU, "mangaryu.com", 10)

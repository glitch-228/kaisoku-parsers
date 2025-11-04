package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("NEWMANHUA", "NewManhua", "en")
internal class NewManhua(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NEWMANHUA, "newmanhua.com", pageSize = 16)

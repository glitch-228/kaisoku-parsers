package org.koitharu.Kaisoku.parsers.site.zeistmanga.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("MANGASOUL", "MangaSoul", "ar")
internal class MangaSoul(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.MANGASOUL, "www.manga-soul.com")

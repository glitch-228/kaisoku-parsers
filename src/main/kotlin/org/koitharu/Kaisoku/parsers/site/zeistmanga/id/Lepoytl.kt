package org.koitharu.Kaisoku.parsers.site.zeistmanga.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("LEPOYTL", "Lepoytl", "id")
internal class Lepoytl(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.LEPOYTL, "www.lepoytl.cloud")

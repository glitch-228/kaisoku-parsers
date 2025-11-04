package org.koitharu.Kaisoku.parsers.site.zeistmanga.id

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser

@Broken
@MangaSourceParser("ASUPANKOMIK", "AsupanKomik", "id")
internal class AsupanKomik(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.ASUPANKOMIK, "www.asupankomik.my.id")

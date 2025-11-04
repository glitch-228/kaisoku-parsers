package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGATX_GG", "MangaTx.gg", "en")
internal class MangaTxGg(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGATX_GG, "mangatx.gg")

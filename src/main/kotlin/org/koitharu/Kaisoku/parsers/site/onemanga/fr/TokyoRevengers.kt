package org.koitharu.Kaisoku.parsers.site.onemanga.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.onemanga.OneMangaParser

@MangaSourceParser("TOKYOREVENGERS", "TokyoRevengers", "fr")
internal class TokyoRevengers(context: MangaLoaderContext) :
	OneMangaParser(context, MangaParserSource.TOKYOREVENGERS, "tokyorevengers.fr")

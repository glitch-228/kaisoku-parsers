package org.koitharu.Kaisoku.parsers.site.onemanga.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.onemanga.OneMangaParser

@Broken
@MangaSourceParser("KAIJUNO8", "KaijuNo8", "fr")
internal class KaijuNo8(context: MangaLoaderContext) :
	OneMangaParser(context, MangaParserSource.KAIJUNO8, "kaijuno8.fr")

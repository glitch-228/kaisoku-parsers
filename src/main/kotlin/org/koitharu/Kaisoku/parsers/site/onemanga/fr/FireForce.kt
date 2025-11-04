package org.koitharu.Kaisoku.parsers.site.onemanga.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.onemanga.OneMangaParser

@MangaSourceParser("FIREFORCE", "FireForce", "fr")
internal class FireForce(context: MangaLoaderContext) :
	OneMangaParser(context, MangaParserSource.FIREFORCE, "fireforce.fr")

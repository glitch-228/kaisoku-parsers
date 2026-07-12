package org.koitharu.kotatsu.parsers.site.hiper.pt

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.hiper.HiperParser

@MangaSourceParser("HIPERTOON", "HiperToon", "pt")
internal class HiperToon(context: MangaLoaderContext) :
	HiperParser(context, MangaParserSource.HIPERTOON, "hipertoon.com")

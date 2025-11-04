package org.koitharu.Kaisoku.parsers.site.mmrcms.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mmrcms.MmrcmsParser

@MangaSourceParser("BANANASCAN_COM", "BananaScan.Com", "en")
internal class BananaScan(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.BANANASCAN_COM, "bananascans.com")

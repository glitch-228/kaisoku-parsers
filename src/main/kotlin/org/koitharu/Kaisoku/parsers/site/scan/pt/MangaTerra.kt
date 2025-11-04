package org.koitharu.Kaisoku.parsers.site.scan.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.scan.ScanParser

@MangaSourceParser("MANGATERRA", "MangaTerra", "pt")
internal class MangaTerra(context: MangaLoaderContext) :
	ScanParser(context, MangaParserSource.MANGATERRA, "manga-terra.com")

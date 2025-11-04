package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("NIRVANASCAN", "NirvanaScan", "pt")
internal class NirvanaScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NIRVANASCAN, "nirvanascan.com")

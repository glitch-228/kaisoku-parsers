package org.koitharu.Kaisoku.parsers.site.madara.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("HARMONYSCAN", "HarmonyScan", "fr")
internal class HarmonyScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HARMONYSCAN, "harmony-scan.fr")

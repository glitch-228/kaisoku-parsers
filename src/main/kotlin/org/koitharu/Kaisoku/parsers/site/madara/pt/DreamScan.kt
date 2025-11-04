package org.koitharu.Kaisoku.parsers.site.madara.pt

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("DREAMSCAN", "DreamScan", "pt")
internal class DreamScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.DREAMSCAN, "fairydream.com.br")

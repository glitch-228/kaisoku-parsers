package org.koitharu.Kaisoku.parsers.site.madara.es

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken
@MangaSourceParser("MMDAOS", "Mmdaos", "es")
internal class Mmdaos(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MMDAOS, "mmdaos.com")

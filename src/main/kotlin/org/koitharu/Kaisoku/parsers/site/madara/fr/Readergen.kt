package org.koitharu.Kaisoku.parsers.site.madara.fr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("READERGEN", "ReaderGen", "fr")
internal class Readergen(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.READERGEN, "fr.readergen.fr", 18)

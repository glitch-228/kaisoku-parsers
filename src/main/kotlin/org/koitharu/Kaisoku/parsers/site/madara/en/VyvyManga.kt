package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken // It has become obsolete and has been replaced by the new VyManga parser.
@MangaSourceParser("VYVYMANGA", "VyvyManga", "en")
internal class VyvyManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.VYVYMANGA, "vyvymanga.org")

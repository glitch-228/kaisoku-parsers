package org.koitharu.Kaisoku.parsers.site.iken.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.iken.IkenParser

@MangaSourceParser("MAGUSMANGA", "MagusToon", "en")
internal class MagusToon(context: MangaLoaderContext) :
        IkenParser(context, MangaParserSource.MAGUSMANGA, "magustoon.org", 18, true)

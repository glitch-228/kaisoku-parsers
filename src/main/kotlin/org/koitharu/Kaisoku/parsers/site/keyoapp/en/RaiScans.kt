package org.koitharu.Kaisoku.parsers.site.keyoapp.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.keyoapp.KeyoappParser

@MangaSourceParser("RAISCANS", "KenScans", "en")
internal class RaiScans(context: MangaLoaderContext) :
	KeyoappParser(context, MangaParserSource.RAISCANS, "kenscans.com")

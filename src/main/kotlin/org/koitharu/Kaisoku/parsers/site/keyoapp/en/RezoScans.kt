package org.koitharu.Kaisoku.parsers.site.keyoapp.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.keyoapp.KeyoappParser

@MangaSourceParser("REZOSCANS", "RezoScans", "en")
internal class RezoScans(context: MangaLoaderContext) :
	KeyoappParser(context, MangaParserSource.REZOSCANS, "rezoscans.com")

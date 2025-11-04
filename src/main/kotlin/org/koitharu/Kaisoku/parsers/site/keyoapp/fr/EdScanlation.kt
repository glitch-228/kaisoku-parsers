package org.koitharu.Kaisoku.parsers.site.keyoapp.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.keyoapp.KeyoappParser

@MangaSourceParser("EDSCANLATION", "EdScanlation", "fr")
internal class EdScanlation(context: MangaLoaderContext) :
	KeyoappParser(context, MangaParserSource.EDSCANLATION, "edscanlation.fr")

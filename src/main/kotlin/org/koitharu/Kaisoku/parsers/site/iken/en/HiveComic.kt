package org.koitharu.Kaisoku.parsers.site.iken.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.iken.IkenParser

@MangaSourceParser("HIVECOMIC", "HiveComic", "en")
internal class HiveComic(context: MangaLoaderContext) :
	IkenParser(context, MangaParserSource.HIVECOMIC, "hivetoons.org", 18, true)

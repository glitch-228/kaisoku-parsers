package org.koitharu.Kaisoku.parsers.site.guya.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.guya.GuyaParser

@MangaSourceParser("HACHIRUMI", "Hachirumi", "en", ContentType.HENTAI)
internal class Hachirumi(context: MangaLoaderContext) :
	GuyaParser(context, MangaParserSource.HACHIRUMI, "hachirumi.com")

package org.koitharu.Kaisoku.parsers.site.zeistmanga.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("HIJALA", "Hijala", "ar")
internal class Hijala(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.HIJALA, "hijala.com")

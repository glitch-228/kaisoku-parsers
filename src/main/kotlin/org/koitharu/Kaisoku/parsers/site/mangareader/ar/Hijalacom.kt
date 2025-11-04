package org.koitharu.Kaisoku.parsers.site.mangareader.ar

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("HIJALACOM", "Hijalacom", "ar")
internal class Hijalacom(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.HIJALACOM, "hijala.com", pageSize = 30, searchPageSize = 10)

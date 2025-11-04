package org.koitharu.Kaisoku.parsers.site.cupfox.vi

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.cupfox.CupFoxParser

@Broken
@MangaSourceParser("OIOIVN", "OioiVn", "vi")
internal class OioiVn(context: MangaLoaderContext) :
	CupFoxParser(context, MangaParserSource.OIOIVN, "oioivn.com")

package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("STKISSMANGABLOG", "1StKissManga.net", "en")
internal class StkissMangaBlog(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.STKISSMANGABLOG, "1stkissmanga.org", 20) {
            override val postReq = true
      }

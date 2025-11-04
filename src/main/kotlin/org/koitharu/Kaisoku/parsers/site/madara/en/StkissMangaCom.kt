package org.koitharu.Kaisoku.parsers.site.madara.en

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser
import org.koitharu.Kaisoku.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("STKISSMANGA_COM", "1stKissManga.com", "en")
internal class StkissMangaCom(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.STKISSMANGA_COM, "1stkissmanga.mom", 10) {
            override val postReq = true
      }

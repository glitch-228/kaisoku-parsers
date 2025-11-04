package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGATILKISI", "MangaTilkisi", "tr")
internal class MangaTilkisi(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGATILKISI, "www.mangatilkisi.net", pageSize = 18) {
	override val datePattern = "dd/MM/yyyy"
  }

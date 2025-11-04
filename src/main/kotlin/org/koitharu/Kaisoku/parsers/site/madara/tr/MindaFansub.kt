package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

@MangaSourceParser("MINDAFANSUB", "MindaFansub", "tr", ContentType.HENTAI)
internal class MindaFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MINDAFANSUB, "mindafansub.online")

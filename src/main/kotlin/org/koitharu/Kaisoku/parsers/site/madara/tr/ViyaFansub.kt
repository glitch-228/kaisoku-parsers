package org.koitharu.Kaisoku.parsers.site.madara.tr

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.madara.MadaraParser

//Manga +18 require login.
@Broken
@MangaSourceParser("VIYAFANSUB", "ViyaFansub", "tr", ContentType.HENTAI)
internal class ViyaFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.VIYAFANSUB, "viyafansub.com")
package org.koitharu.Kaisoku.parsers.site.heancms.es

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.*
import org.koitharu.Kaisoku.parsers.site.heancms.HeanCms

@Broken("Not dead, changed template")
@MangaSourceParser("YUGEN_MANGAS_ES", "YugenMangas.lat", "es", ContentType.HENTAI)
internal class YugenMangasEs(context: MangaLoaderContext) :
	HeanCms(context, MangaParserSource.YUGEN_MANGAS_ES, "lectorikigai.acamu.net")

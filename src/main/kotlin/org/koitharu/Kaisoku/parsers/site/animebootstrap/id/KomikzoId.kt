package org.koitharu.Kaisoku.parsers.site.animebootstrap.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.animebootstrap.AnimeBootstrapParser

@MangaSourceParser("KOMIKZOID", "KomikzoId", "id")
internal class KomikzoId(context: MangaLoaderContext) :
	AnimeBootstrapParser(context, MangaParserSource.KOMIKZOID, "komikzoid.id")

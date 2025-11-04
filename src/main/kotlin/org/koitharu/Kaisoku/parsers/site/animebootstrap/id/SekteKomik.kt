package org.koitharu.Kaisoku.parsers.site.animebootstrap.id

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.animebootstrap.AnimeBootstrapParser

@MangaSourceParser("SEKTEKOMIK", "SekteKomik", "id")
internal class SekteKomik(context: MangaLoaderContext) :
	AnimeBootstrapParser(context, MangaParserSource.SEKTEKOMIK, "sektekomik.xyz")

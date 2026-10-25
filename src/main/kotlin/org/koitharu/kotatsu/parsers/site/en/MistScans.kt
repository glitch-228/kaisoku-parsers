package org.koitharu.kotatsu.parsers.site.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.site.keyoapp.PagedKeyoParser

import org.koitharu.kotatsu.parsers.model.MangaParserSource

@MangaSourceParser("MISTSCANS", "Mist Scans", "en")
internal class MistScans(context: MangaLoaderContext) :
    PagedKeyoParser(context, MangaParserSource.MISTSCANS, "mistscans.com", pageSize = 20) {
    override fun popularMangaSelector(): String = ".series-splide .splide__slide"
}

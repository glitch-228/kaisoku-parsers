package org.koitharu.kotatsu.parsers.site.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.site.keyoapp.PagedKeyoParser

import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.SortOrder

@MangaSourceParser("GENZTOONS", "Genz Toons", "en")
internal class GenzToons(context: MangaLoaderContext) :
    PagedKeyoParser(context, MangaParserSource.GENZTOONS, "genztoons.org") {

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val effectiveOrder = if (order == SortOrder.POPULARITY) SortOrder.RELEVANCE else order
        return super.getListPage(page, effectiveOrder, filter)
    }
}

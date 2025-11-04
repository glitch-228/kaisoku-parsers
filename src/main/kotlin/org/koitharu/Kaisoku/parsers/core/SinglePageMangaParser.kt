package org.koitharu.Kaisoku.parsers.core

import org.koitharu.Kaisoku.parsers.InternalParsersApi
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.model.Manga
import org.koitharu.Kaisoku.parsers.model.MangaListFilter
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.model.SortOrder

@InternalParsersApi
public abstract class SinglePageMangaParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
) : AbstractMangaParser(context, source) {

	final override suspend fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (offset > 0) {
			return emptyList()
		}
		return getList(order, filter)
	}

	public abstract suspend fun getList(order: SortOrder, filter: MangaListFilter): List<Manga>
}

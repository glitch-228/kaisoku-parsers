package org.koitharu.Kaisoku.parsers.site.gallery.zh

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.config.ConfigKey
import org.koitharu.Kaisoku.parsers.model.ContentType
import org.koitharu.Kaisoku.parsers.model.MangaListFilterOptions
import org.koitharu.Kaisoku.parsers.site.gallery.GalleryParser

@MangaSourceParser("XIUTAKU", "Xiutaku", "zh", type = ContentType.OTHER)
internal class Xiutaku(context: MangaLoaderContext) :
    GalleryParser(context, MangaParserSource.XIUTAKU, "xiutaku.com") {
    
    override suspend fun getFilterOptions():
        MangaListFilterOptions = MangaListFilterOptions(availableTags = fetchTags())

}
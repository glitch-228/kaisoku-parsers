package org.koitharu.Kaisoku.parsers.site.en.mtl

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.model.ContentType

@Broken
@MangaSourceParser("SNOWMTL", "SnowMTL", "en", type = ContentType.OTHER)
internal class SnowMTL(context: MangaLoaderContext):
    MTLParser(context, source = MangaParserSource.SNOWMTL, "snowmtl.ru")

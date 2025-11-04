package org.koitharu.Kaisoku.parsers.site.keyoapp.en

import org.koitharu.Kaisoku.parsers.Broken
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.keyoapp.KeyoappParser

@Broken
@MangaSourceParser("LUASCANS", "luaComic.net", "en")
internal class LuaScans(context: MangaLoaderContext) :
	KeyoappParser(context, MangaParserSource.LUASCANS, "luacomic.org")

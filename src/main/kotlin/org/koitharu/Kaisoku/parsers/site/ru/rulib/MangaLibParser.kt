package org.koitharu.Kaisoku.parsers.site.ru.rulib

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.exception.AuthRequiredException
import org.koitharu.Kaisoku.parsers.exception.NotFoundException
import org.koitharu.Kaisoku.parsers.model.MangaChapter
import org.koitharu.Kaisoku.parsers.model.MangaPage
import org.koitharu.Kaisoku.parsers.model.MangaParserSource

@MangaSourceParser("MANGALIB", "MangaLib", "ru")
internal class MangaLibParser(
	context: MangaLoaderContext,
) : LibSocialParser(
	context = context,
	source = MangaParserSource.MANGALIB,
	siteId = 1,
	siteDomains = arrayOf("mangalib.org", "mangalib.me"),
) {

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = try {
		super.getPages(chapter)
	} catch (e: NotFoundException) {
		throw AuthRequiredException(source, e)
	}
}

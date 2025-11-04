package org.koitharu.Kaisoku.parsers.site.ru.grouple

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.config.ConfigKey
import org.koitharu.Kaisoku.parsers.model.MangaParserSource

@MangaSourceParser("USAGI", "Usagi", "ru")
internal class UsagiParser(
	context: MangaLoaderContext,
) : GroupleParser(context, MangaParserSource.USAGI, 1) {

	override val configKeyDomain = ConfigKey.Domain(*domains)

	override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
		.add("referer", "https://$domain/")
		.build()

	companion object {

		val domains = arrayOf("web.usagi.one")
	}
}

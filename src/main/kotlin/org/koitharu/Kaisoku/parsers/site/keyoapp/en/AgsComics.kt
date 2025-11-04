package org.koitharu.Kaisoku.parsers.site.keyoapp.en

import org.jsoup.nodes.Element
import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.keyoapp.KeyoappParser
import org.koitharu.Kaisoku.parsers.util.styleValueOrNull
import org.koitharu.Kaisoku.parsers.util.cssUrl
import org.koitharu.Kaisoku.parsers.Broken

@Broken // It seems like the site is dead.
@MangaSourceParser("AGSCOMICS", "AgsComics", "en")
internal class AgsComics(context: MangaLoaderContext) :
	KeyoappParser(context, MangaParserSource.AGSCOMICS, "agrcomics.com") {

	override val cover: (Element) -> String? = { div ->
		val coverDiv = div.selectFirst("div.bg-cover[style*=background-image]")
			?: div.takeIf { it.hasClass("bg-cover") && it.hasAttr("style") }
			?: div.selectFirst("[style*=background-image]")
			?: throw Exception("Element or image url not found")

		coverDiv.styleValueOrNull("background-image")?.cssUrl()
	}

}

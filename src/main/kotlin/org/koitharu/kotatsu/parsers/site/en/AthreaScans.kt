package org.koitharu.kotatsu.parsers.site.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.ThemesiaParser

import org.jsoup.nodes.Element

@MangaSourceParser("ATHREASCANS", "AthreaScans", "en")
internal class AthreaScans(context: MangaLoaderContext) :
    ThemesiaParser(context, MangaParserSource.ATHREASCANS, "athreascans.com") {

    override fun transformChapterName(element: Element, name: String): String {
        return if (element.selectFirst("a[data-bs-target='#lockedChapterModal']") != null) {
            "🔒 $name"
        } else {
            name
        }
    }
}

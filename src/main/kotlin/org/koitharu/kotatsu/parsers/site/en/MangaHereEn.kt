package org.koitharu.kotatsu.parsers.site.en

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*

/** The mobile reader declares the complete image list, including its signed query strings. */
@MangaSourceParser("MANGAHERE_EN", "MangaHere", "en")
internal class MangaHereEn(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.MANGAHERE_EN, 20) {

    override val configKeyDomain = ConfigKey.Domain("m.mangahere.cc")
    override val availableSortOrders = setOf(SortOrder.UPDATED, SortOrder.POPULARITY)
    override val filterCapabilities = MangaListFilterCapabilities(isSearchSupported = true)
    override suspend fun getFilterOptions() = MangaListFilterOptions()

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        // These mobile pages have a single visible result list and no advertised next-page link.
        if (page > 1) return emptyList()
        val url = "https://$domain/".toHttpUrl().newBuilder()
        when {
            !filter.query.isNullOrBlank() -> url.addPathSegment("search").addQueryParameter("query", filter.query)
            order == SortOrder.POPULARITY -> url.addPathSegment("hot")
        }
        val doc = webClient.httpGet(url.build()).parseHtml()
        return doc.select(".manga-list .post > a:has(.title)").map { anchor ->
            val href = anchor.attrAsRelativeUrl("href")
            Manga(
                id = generateUid(href), url = href, publicUrl = href.toAbsoluteUrl(domain),
                title = anchor.selectFirstOrThrow(".title").text(), coverUrl = anchor.selectFirst("img")?.src(),
                altTitles = emptySet(), rating = RATING_UNKNOWN, tags = emptySet(), authors = emptySet(),
                state = null, contentRating = null, source = source,
            )
        }.distinctBy { it.id }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
        val title = doc.selectFirst(".manga-detail-top > .title")?.textOrNull()
            ?: throw ParseException("Series details not found", doc.location())
        val chapters = doc.select(".manga-chapters a[href]").distinctBy { it.attr("href") }.asReversed().map { anchor ->
            val href = anchor.attrAsRelativeUrl("href")
            MangaChapter(
                id = generateUid(href), url = href, title = anchor.text(), volume = 0,
                number = anchor.text().removePrefix("C.").toFloatOrNull() ?: 0f,
                uploadDate = 0L, scanlator = null, branch = null, source = source,
            )
        }
        if (chapters.size > 20_000) throw ParseException("Chapter list is too large", doc.location())
        return manga.copy(
            title = title, coverUrl = doc.selectFirst(".detail-cover")?.src() ?: manga.coverUrl,
            authors = doc.select(".detail-info a[href*=/author/]").mapToSet { it.text() },
            description = doc.selectFirst(".manga-summary")?.textOrNull(),
            tags = doc.select(".manga-genres a[href]").mapToSet {
                MangaTag(title = it.text(), key = it.attrAsRelativeUrl("href"), source = source)
            },
            state = doc.select(".detail-info p").firstOrNull { it.text().startsWith("Status:") }?.text()?.let {
                when {
                    it.contains("Ongoing", true) -> MangaState.ONGOING
                    it.contains("Completed", true) -> MangaState.FINISHED
                    else -> null
                }
            } ?: manga.state,
            chapters = chapters,
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val saved = chapter.url.toAbsoluteUrl(domain).toHttpUrl()
        if (saved.pathSegments.firstOrNull() != "manga") throw ParseException("Invalid chapter URL", chapter.url)
        val url = saved.newBuilder().setPathSegment(0, "roll_manga").build()
        val doc = webClient.httpGet(url).parseHtml()
        val pages = doc.select("#viewer img[data-original]").map { image ->
            val imageUrl = image.attr("data-original").toAbsoluteUrl(domain).toHttpUrl()
            MangaPage(id = generateUid(imageUrl.toString()), url = imageUrl.toString(), preview = null, source = source)
        }
        if (pages.isEmpty() || pages.size > 5_000) throw ParseException("Chapter images are unavailable", doc.location())
        return pages
    }
}

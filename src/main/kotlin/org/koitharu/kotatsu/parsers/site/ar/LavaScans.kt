package org.koitharu.kotatsu.parsers.site.ar

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import java.text.SimpleDateFormat
import java.util.Locale

/** Current card/detail selectors follow the complete public Keiyoushi and UMA adapters. */
@MangaSourceParser("LAVASCANS", "Lava Scans", "ar")
internal class LavaScans(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.LAVASCANS, 32) {

    override val configKeyDomain = ConfigKey.Domain("lavascans.com")
    override val availableSortOrders = setOf(SortOrder.UPDATED, SortOrder.POPULARITY, SortOrder.NEWEST)
    override val filterCapabilities = MangaListFilterCapabilities(isSearchSupported = true)
    override suspend fun getFilterOptions() = MangaListFilterOptions()

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val search = !filter.query.isNullOrBlank()
        val url = "https://$domain/".toHttpUrl().newBuilder()
        if (search) {
            if (page > 1) url.addPathSegments("page/$page/")
            url.addQueryParameter("s", filter.query)
        } else {
            url.addPathSegments("manga/").addQueryParameter("page", page.toString())
                .addQueryParameter("order", when (order) {
                    SortOrder.POPULARITY -> "popular"
                    SortOrder.NEWEST -> "latest"
                    else -> "update"
                })
        }
        val doc = webClient.httpGet(url.build()).parseHtml()
        val cards = doc.select(if (search) ".legend-card" else ".listupd .manga-card-v")
        return cards.mapNotNull { card ->
            val anchor = card.selectFirst(if (search) ".legend-title a" else "h3.card-v-title a") ?: return@mapNotNull null
            val href = anchor.attrAsRelativeUrl("href")
            if (!href.startsWith("/manga/")) return@mapNotNull null
            val title = anchor.text().ifBlank { anchor.attr("title") }
            if (title.isBlank()) return@mapNotNull null
            Manga(
                id = generateUid(href), url = href, publicUrl = href.toAbsoluteUrl(domain), title = title,
                coverUrl = card.selectFirst(if (search) ".legend-poster img" else ".card-v-cover img")?.src(),
                altTitles = emptySet(), authors = emptySet(), tags = emptySet(), state = null,
                rating = RATING_UNKNOWN, contentRating = null, source = source,
            )
        }.distinctBy { it.id }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
        val details = doc.selectFirst(".lh-container") ?: throw ParseException("Comic details not found", doc.location())
        val chapters = doc.select("#chapters-list-container .ch-item").map { row ->
            // Locked chapters remain part of the confirmed list. Their reader returns an access error.
            val anchor = row.selectFirstOrThrow("a.ch-main-anchor")
            val href = anchor.attrAsRelativeUrl("href")
            val title = row.selectFirst(".ch-num")?.textOrNull() ?: anchor.text()
            MangaChapter(
                id = generateUid(href), url = href, title = title,
                number = row.attr("data-ch").toFloatOrNull() ?: title.extractChapterNumber(),
                volume = 0, scanlator = null, branch = null,
                uploadDate = SimpleDateFormat("yyyy/MM/dd", Locale.ROOT).parseSafe(row.selectFirst(".ch-date")?.text()),
                source = source,
            )
        }.distinctBy { it.id }.sortedBy { it.number }
        if (chapters.size > 20_000) throw ParseException("Chapter list is too large", doc.location())
        return manga.copy(
            title = details.selectFirst(".lh-title")?.textOrNull() ?: manga.title,
            coverUrl = details.selectFirst(".lh-poster img")?.src() ?: manga.coverUrl,
            description = details.selectFirst("#manga-story")?.textOrNull(),
            tags = details.select(".lh-genres a").mapToSet {
                MangaTag(title = it.text(), key = it.attrAsRelativeUrl("href"), source = source)
            },
            state = details.selectFirst(".status-badge-lux")?.text()?.let {
                when (it.lowercase(Locale.ROOT)) {
                    "مستمر", "ongoing" -> MangaState.ONGOING
                    "مكتمل", "completed" -> MangaState.FINISHED
                    else -> null
                }
            } ?: manga.state,
            chapters = chapters,
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
        val pages = doc.select("#readerarea img").map { image ->
            val url = image.requireSrc()
            MangaPage(id = generateUid(url), url = url, preview = null, source = source)
        }
        if (pages.isEmpty() || pages.size > 5_000) {
            throw ParseException("Comic images are unavailable; chapter may be locked or text only", doc.location())
        }
        return pages
    }
}

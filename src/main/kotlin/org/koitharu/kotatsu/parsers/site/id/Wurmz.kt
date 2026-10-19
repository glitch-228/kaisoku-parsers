package org.koitharu.kotatsu.parsers.site.id

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.model.SortOrder

import org.koitharu.kotatsu.parsers.util.attrAsRelativeUrl
import org.koitharu.kotatsu.parsers.util.extractChapterNumber
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.src
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl

import org.jsoup.nodes.Element
import java.util.EnumSet

/** Source contract adapted from UMA f207e8e; reader URLs are never inferred. */
@MangaSourceParser("WURMZ", "Wurmz", "id")
internal class Wurmz(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.WURMZ, pageSize = 30) {

    override val configKeyDomain = ConfigKey.Domain("wurmz.net")

    override val availableSortOrders: Set<SortOrder> = setOf(
        SortOrder.UPDATED,
        SortOrder.ADDED,
        SortOrder.ADDED_ASC,
        SortOrder.POPULARITY_TODAY,
        SortOrder.POPULARITY_WEEK,
        SortOrder.POPULARITY_MONTH,
        SortOrder.POPULARITY,
    )

    override val filterCapabilities = MangaListFilterCapabilities(
        isSearchSupported = true,
        isMultipleTagsSupported = true,
        isTagsExclusionSupported = false,
        isSearchWithFiltersSupported = true,
    )

    override suspend fun getFilterOptions(): MangaListFilterOptions {
        return MangaListFilterOptions(
            availableTags = GENRES.map { genre ->
                MangaTag(key = genre, title = genre, source = source)
            }.toSet(),
            availableStates = EnumSet.of(
                MangaState.ONGOING,
                MangaState.FINISHED,
                MangaState.PAUSED,
                MangaState.ABANDONED,
            ),
            availableContentTypes = EnumSet.of(
                ContentType.MANGA,
                ContentType.MANHWA,
                ContentType.MANHUA,
            ),
        )
    }

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = buildListUrl(page, order, filter)
        val doc = webClient.httpGet(url).parseHtml()

        val cards = doc.select("article.comic-card")
        if (cards.isEmpty()) {
            return emptyList()
        }
        return cards.mapNotNull { parseMangaCard(it) }
    }

    private fun buildListUrl(page: Int, order: SortOrder, filter: MangaListFilter): String {
        val url = "https://$domain/semua-komik".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("sort", when (order) {
                SortOrder.ADDED -> "new"
                SortOrder.ADDED_ASC -> "old"
                SortOrder.POPULARITY_TODAY -> "popular_today"
                SortOrder.POPULARITY_WEEK -> "popular_7d"
                SortOrder.POPULARITY_MONTH -> "popular_30d"
                SortOrder.POPULARITY -> "popular_all"
                else -> "update"
            })
        filter.query?.takeIf(String::isNotBlank)?.let { url.addQueryParameter("q", it) }
        filter.types.firstOrNull()?.let { type ->
            url.addQueryParameter("type", when (type) {
                ContentType.MANGA -> "manga"
                ContentType.MANHWA -> "manhwa"
                ContentType.MANHUA -> "manhua"
                else -> return@let
            })
        }
        filter.states.firstOrNull()?.let { state ->
            url.addQueryParameter("status", when (state) {
                MangaState.ONGOING -> "ongoing"
                MangaState.FINISHED -> "tamat"
                MangaState.PAUSED -> "hiatus"
                MangaState.ABANDONED -> "drop"
                else -> return@let
            })
        }
        filter.tags.forEach { url.addQueryParameter("genres[]", it.key) }
        return url.build().toString()
    }

    private fun parseMangaCard(card: Element): Manga? {
        val link = card.selectFirst("a[href]") ?: return null
        val href = link.attrAsRelativeUrl("href")
        val titleEl = card.selectFirst("h2.comic-title")
        val title = titleEl?.text() ?: return null
        val coverEl = card.selectFirst("img")
        val coverUrl = coverEl?.src()?.toAbsoluteUrl(domain)

        return Manga(
            id = generateUid(href),
            url = href,
            publicUrl = href.toAbsoluteUrl(domain),
            title = title,
            altTitles = emptySet(),
            coverUrl = coverUrl,
            authors = emptySet(),
            tags = emptySet(),
            rating = RATING_UNKNOWN,
            state = null,
            contentRating = null,
            source = source,
        )
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val fullUrl = manga.url.toAbsoluteUrl(domain)
        val doc = webClient.httpGet(fullUrl).parseHtml()

        val title = doc.selectFirst("h1")?.text() ?: manga.title
        val cover = doc.selectFirst(".cover-frame img")?.src()?.toAbsoluteUrl(domain) ?: manga.coverUrl

        val desc = doc.selectFirst(".mt-5 p.text-muted")?.text() ?: ""

        val genreElements = doc.select(".flex.flex-wrap.gap-1.5 .chip")
        val tags = genreElements.mapNotNull { a ->
            val genreName = a.text()
            MangaTag(key = genreName, title = genreName, source = source)
        }.toSet()

        val statusText = doc.selectFirst(".status-badge")?.text()
        val state = when (statusText?.lowercase()) {
            "ongoing" -> MangaState.ONGOING
            "completed" -> MangaState.FINISHED
            "hiatus" -> MangaState.PAUSED
            "cancelled" -> MangaState.ABANDONED
            else -> null
        }

        val authorText = doc.selectFirst(".text-xs.text-muted.mt-2.5")?.text()
        val authors = authorText?.substringAfter("Oleh ")?.substringBefore(" ·")?.split('|')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && it != "-?" }
            ?.toSet() ?: emptySet()

        val altText = doc.selectFirst(".text-sm.text-faint.mt-1")?.text()
        val altTitles = altText?.split("·")?.map { it.trim() }?.toSet() ?: emptySet()

        val chapterLinks = doc.select(".chap-cell")
        val chapters = chapterLinks.map { a ->
            val href = a.attrAsRelativeUrl("href")
            val label = a.text().trim()
            val number = label.extractChapterNumber()
            MangaChapter(
                id = generateUid(href),
                url = href,
                title = label,
                number = number,
                volume = 0,
                uploadDate = 0,
                source = source,
                scanlator = null,
                branch = null,
            )
        }.sortedBy { it.number }

        return manga.copy(
            title = title,
            publicUrl = fullUrl,
            coverUrl = cover,
            description = desc,
            tags = tags,
            state = state,
            authors = authors,
            altTitles = altTitles,
            contentRating = if (tags.any { it.title.lowercase() in setOf("adult", "hentai", "18+") })
                ContentRating.ADULT else manga.contentRating,
            chapters = chapters,
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val fullUrl = chapter.url.toAbsoluteUrl(domain)
        val doc = webClient.httpGet(fullUrl).parseHtml()

        val urls = doc.select(".reader-page img").mapNotNull { it.src()?.takeIf(String::isNotBlank) }
            .map { it.toAbsoluteUrl(domain) }.distinct()
        if (urls.isEmpty()) throw ParseException("Reader image URLs not found", fullUrl)
        return urls.map { MangaPage(id = generateUid(it), url = it, preview = null, source = source) }
    }

    companion object {
        private val GENRES = listOf(
            "Fantasy", "Romance", "Drama", "Action", "Comedy", "Shounen", "Adventure", "Shoujo",
            "School", "Slice of Life", "Seinen", "Supernatural", "Historical", "Josei", "Isekai",
            "Webtoons", "Martial Art", "Harem", "Ecchi", "Reincarnation", "Magic", "Mystery",
            "Psychological", "Yaoi", "Mature", "Horror", "Tragedy", "Shounen Ai", "Sci-fi",
            "Demons", "Adaptation", "Gender Bender", "Game", "Yuri", "Shoujo Ai", "Thriller",
            "Sport", "Cooking", "Crime", "Gore", "Royal Family", "Super Power", "Regression",
            "Military", "Royalty", "Vampire", "Office Workers", "Transmigration", "Medical",
            "Time Travel", "Childhood Friends", "Music", "Monsters", "Revenge", "College Life",
            "Villainess", "Kids", "Mecha", "One Shot", "Police", "Omegaverse", "Girls",
            "Reverse Harem", "Animals", "Ghosts", "Project", "Age Gap", "Showbiz", "Violence",
            "Boys", "Shotacon", "Survival", "Beasts", "Bloody", "Crossdressing", "Delinguents",
            "Dungeons", "Gyaru", "Non-human", "Post-Apocalyptic", "Zombies", "Doujinshi",
            "18+", "Bodyswap", "NTR", "Yakuzas", "BDSM", "Lolicon", "Philosophical", "System",
            "Aliens", "Hentai", "Anthology", "Fetish", "Incest", "Virtual Reality", "Dementia",
            "Nakadashi", "Samurai", "4-Koma", "Cheating", "Femdom", "Mafia", "Milf", "Ninja",
            "Cunnilingus", "Guideverse", "Infidelity", "Parodi", "Reverse Isekai", "Villain"
        )
    }
}

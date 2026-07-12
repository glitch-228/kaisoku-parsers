package org.koitharu.kotatsu.parsers.site.madara.en

import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser
import org.koitharu.kotatsu.parsers.util.*
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("AQUAMANGA", "AquaManga", "en")
internal class AquaManga(context: MangaLoaderContext) :
    MadaraParser(context, MangaParserSource.AQUAMANGA, "aquareader.org", 20) {

    override val withoutAjax = true
    override val stylePage = ""

    override val selectChapter = ".aqua-ch-item"

    override val availableSortOrders: EnumSet<SortOrder> = EnumSet.of(
        SortOrder.UPDATED,
        SortOrder.POPULARITY,
        SortOrder.NEWEST,
        SortOrder.ALPHABETICAL,
    )

    override suspend fun getFilterOptions() = MangaListFilterOptions(
        availableTags = setOf(
            MangaTag(key = "academy", title = "Academy", source = source),
            MangaTag(key = "action", title = "Action", source = source),
            MangaTag(key = "adaptation", title = "Adaptation", source = source),
            MangaTag(key = "adventure", title = "Adventure", source = source),
            MangaTag(key = "comedy", title = "Comedy", source = source),
            MangaTag(key = "cooking", title = "Cooking", source = source),
            MangaTag(key = "crime", title = "Crime", source = source),
            MangaTag(key = "cultivation", title = "Cultivation", source = source),
            MangaTag(key = "delinquents", title = "Delinquents", source = source),
            MangaTag(key = "demons", title = "Demons", source = source),
            MangaTag(key = "drama", title = "Drama", source = source),
            MangaTag(key = "dungeons", title = "Dungeons", source = source),
            MangaTag(key = "ecchi", title = "Ecchi", source = source),
            MangaTag(key = "fantasy", title = "Fantasy", source = source),
            MangaTag(key = "game", title = "Game", source = source),
            MangaTag(key = "gore", title = "Gore", source = source),
            MangaTag(key = "harem", title = "Harem", source = source),
            MangaTag(key = "historical", title = "Historical", source = source),
            MangaTag(key = "horror", title = "Horror", source = source),
            MangaTag(key = "isekai", title = "Isekai", source = source),
            MangaTag(key = "josei", title = "Josei", source = source),
            MangaTag(key = "magic", title = "Magic", source = source),
            MangaTag(key = "manga", title = "Manga", source = source),
            MangaTag(key = "manhua", title = "Manhua", source = source),
            MangaTag(key = "manhwa", title = "Manhwa", source = source),
            MangaTag(key = "martial-arts", title = "Martial Arts", source = source),
            MangaTag(key = "mecha", title = "Mecha", source = source),
            MangaTag(key = "medical", title = "Medical", source = source),
            MangaTag(key = "military", title = "Military", source = source),
            MangaTag(key = "monsters", title = "Monsters", source = source),
            MangaTag(key = "murim", title = "Murim", source = source),
            MangaTag(key = "music", title = "Music", source = source),
            MangaTag(key = "mystery", title = "Mystery", source = source),
            MangaTag(key = "necromancer", title = "Necromancer", source = source),
            MangaTag(key = "ninja", title = "Ninja", source = source),
            MangaTag(key = "office-workers", title = "Office Workers", source = source),
            MangaTag(key = "op-mc", title = "OP-MC", source = source),
            MangaTag(key = "overpowered", title = "Overpowered", source = source),
            MangaTag(key = "philosophical", title = "Philosophical", source = source),
            MangaTag(key = "post-apocalyptic", title = "Post-Apocalyptic", source = source),
            MangaTag(key = "psychological", title = "Psychological", source = source),
            MangaTag(key = "rebirth", title = "Rebirth", source = source),
            MangaTag(key = "regression", title = "Regression", source = source),
            MangaTag(key = "reincarnation", title = "Reincarnation", source = source),
            MangaTag(key = "returner", title = "Returner", source = source),
            MangaTag(key = "revenge", title = "Revenge", source = source),
            MangaTag(key = "romance", title = "Romance", source = source),
            MangaTag(key = "school-life", title = "School Life", source = source),
            MangaTag(key = "sci-fi", title = "Sci-fi", source = source),
            MangaTag(key = "seinen", title = "Seinen", source = source),
            MangaTag(key = "shounen", title = "Shounen", source = source),
            MangaTag(key = "slice-of-life", title = "Slice-of-Life", source = source),
            MangaTag(key = "sports", title = "Sports", source = source),
            MangaTag(key = "super-power", title = "Super Power", source = source),
            MangaTag(key = "superhero", title = "Superhero", source = source),
            MangaTag(key = "supernatural", title = "Supernatural", source = source),
            MangaTag(key = "survival", title = "Survival", source = source),
            MangaTag(key = "system", title = "System", source = source),
            MangaTag(key = "thriller", title = "Thriller", source = source),
            MangaTag(key = "time-travel", title = "Time Travel", source = source),
            MangaTag(key = "tower", title = "Tower", source = source),
            MangaTag(key = "tragedy", title = "Tragedy", source = source),
            MangaTag(key = "vampire", title = "Vampire", source = source),
            MangaTag(key = "video-games", title = "Video Games", source = source),
            MangaTag(key = "villainess", title = "Villainess", source = source),
            MangaTag(key = "virtual-reality", title = "Virtual Reality", source = source),
            MangaTag(key = "voilence", title = "Voilence", source = source),
            MangaTag(key = "webcomic", title = "Webcomic", source = source),
            MangaTag(key = "wuxia", title = "Wuxia", source = source),
            MangaTag(key = "zombies", title = "Zombies", source = source),
        ),
        availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED),
    )

    override suspend fun getDetails(manga: Manga): Manga {
        val fullUrl = manga.url.toAbsoluteUrl(domain)
        val doc = webClient.httpGet(fullUrl).parseHtml()

        val title = doc.selectFirstOrThrow(".aqua-series-info__title").text()
        val thumbnail = doc.selectFirstOrThrow(".aqua-series-cover__img").requireSrc()
        val description = doc.selectFirst(".aqua-series-synopsis")?.html().orEmpty()
        val status = doc.selectFirst(".aqua-series-meta__status")?.text()
        val genres = doc.select(".aqua-series-genre-pill").map { it.text() }.toSet()
        val authors = doc.select(".aqua-series-info__creator-value a").mapToSet { it.ownText() }

        val tags = genres.mapTo(mutableSetOf()) {
            MangaTag(title = it, key = it.lowercase().replace(' ', '-'), source = source)
        }
        val statusText = status?.lowercase().orEmpty()
        val state = when (statusText) {
            in ongoing -> MangaState.ONGOING
            in finished -> MangaState.FINISHED
            in abandoned -> MangaState.ABANDONED
            in paused -> MangaState.PAUSED
            else -> null
        }

        val chapters = getChapters(manga, doc)
        return manga.copy(
            title = title,
            coverUrl = thumbnail,
            description = description,
            tags = tags,
            state = state,
            authors = authors.filterNot { it.isBlank() }.toSet(),
            chapters = chapters,
        )
    }

    override suspend fun getChapters(manga: Manga, doc: Document): List<MangaChapter> {
        return doc.select(selectChapter).mapChapters(reversed = true) { i, el ->
            val a = el.selectFirstOrThrow("a")
            val href = a.attrAsRelativeUrl("href")
            val name = el.selectFirstOrThrow(".aqua-ch-item__name").text()
            val dateText = el.selectFirst(".aqua-ch-item__time")?.text()?.trim()
            MangaChapter(
                id = generateUid(href),
                url = href + stylePage,
                title = name,
                number = i + 1f,
                volume = 0,
                uploadDate = parseChapterDate(dateText),
                source = source,
                scanlator = null,
                branch = null,
            )
        }
    }

    private fun parseChapterDate(text: String?): Long {
        if (text.isNullOrBlank()) return 0L

        val relativeRegex = Regex(
            """(\d+)\s+(years?|months?|weeks?|days?|hours?|mins?|minutes?|sec(?:onds?)?)\s+ago""",
            RegexOption.IGNORE_CASE
        )
        val match = relativeRegex.matchEntire(text)
        if (match != null) {
            val number = match.groupValues[1].toInt()
            val unit = match.groupValues[2].lowercase()
            val cal = Calendar.getInstance()
            when {
                unit.startsWith("year")   -> cal.add(Calendar.YEAR, -number)
                unit.startsWith("month")  -> cal.add(Calendar.MONTH, -number)
                unit.startsWith("week")   -> cal.add(Calendar.DAY_OF_MONTH, -number * 7)
                unit.startsWith("day")    -> cal.add(Calendar.DAY_OF_MONTH, -number)
                unit.startsWith("hour")   -> cal.add(Calendar.HOUR, -number)
                unit.startsWith("min")    -> cal.add(Calendar.MINUTE, -number)
                unit.startsWith("sec")    -> cal.add(Calendar.SECOND, -number)
            }
            return cal.timeInMillis
        }

        val formats = listOf(
            SimpleDateFormat("MMM d, yyyy", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("MMM dd, yyyy", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("MMMM d, yyyy", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") },
        )
        for (fmt in formats) {
            try {
                return fmt.parse(text)?.time ?: 0L
            } catch (_: Exception) {}
        }

        return 0L
    }
}

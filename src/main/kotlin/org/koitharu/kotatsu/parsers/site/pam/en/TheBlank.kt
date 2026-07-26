package org.koitharu.kotatsu.parsers.site.pam.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.pam.PamParser

@MangaSourceParser("THEBLANK", "TheBlank", "en", ContentType.HENTAI)
internal class TheBlank(context: MangaLoaderContext) :
	PamParser(context, MangaParserSource.THEBLANK, "theblank.net") {

	override val genres = listOf(
		"Action" to "action",
		"Adventure" to "adventure",
		"Ai" to "ai",
		"Animated" to "animated",
		"Anthology" to "anthology",
		"Cohabitation" to "cohabitation",
		"College" to "college",
		"Comedy" to "comedy",
		"Doujinshi" to "doujinshi",
		"Drama" to "drama",
		"Fantasy" to "fantasy",
		"Folklore" to "folklore",
		"Harem" to "harem",
		"Historical" to "historical",
		"Horror" to "horror",
		"Isekai" to "isekai",
		"Josei" to "josei",
		"Love triangle" to "love-triangle",
		"Martial arts" to "martial-arts",
		"Mature" to "mature",
		"Murim" to "murim",
		"Mystery" to "mystery",
		"Office workers" to "office-workers",
		"Psychological" to "psychological",
		"Robots" to "robots",
		"Romance" to "romance",
		"School life" to "school-life",
		"Sci-fi" to "sci-fi",
		"Seinen" to "seinen",
		"Shoujo" to "shoujo",
		"Shounen" to "shounen",
		"Slice of life" to "slice-of-life",
		"Smut" to "smut",
		"Sports" to "sports",
		"Supernatural" to "supernatural",
		"Superpower" to "superpower",
		"System" to "system",
		"Thriller" to "thriller",
		"Uncensored" to "uncensored",
		"Violence" to "violence",
		"Workplace" to "workplace",
	)
}

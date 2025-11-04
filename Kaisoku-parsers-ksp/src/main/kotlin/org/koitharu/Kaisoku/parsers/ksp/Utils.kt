package org.koitharu.Kaisoku.parsers.ksp

import java.util.*

fun String.toTitleCase(locale: Locale?): String {
	return replaceFirstChar { x -> x.uppercase(locale ?: Locale.ROOT) }
}
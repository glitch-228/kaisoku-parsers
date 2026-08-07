package org.koitharu.kotatsu.parsers.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ChaptersTest {

	@ParameterizedTest
	@CsvSource(
		"'Chapter 12', 12.0",
		"'Ch. 12.5', 12.5",
		"'Chapitre 7', 7.0",
		"'Capitulo 3', 3.0",
		"'Capítulo 3,5', 3.5",
		"'Episode 9', 9.0",
		"'Ep. 4', 4.0",
	)
	fun `extracts common localized chapter labels`(title: String, expected: Float) {
		assertEquals(expected, title.extractChapterNumber())
	}

	@ParameterizedTest
	@CsvSource(
		"'19 Days - Chapter 500', 500.0",
		"'#42 - The answer', 42.0",
		"'One shot', 0.0",
	)
	fun `prefers a chapter label and falls back to the first number`(title: String, expected: Float) {
		assertEquals(expected, title.extractChapterNumber())
	}
}

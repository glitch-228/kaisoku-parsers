package org.koitharu.kotatsu.parsers.site.en

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.util.requireSrc

class DemonicScansTest {

	@Test
	fun pageSelectorExcludesReaderAdvertisement() {
		val document = Jsoup.parse(
			"""
				<div>
					<img class="imgholder" src="/img/free_ads.jpg">
					<img class="imgholder" alt="Series Chapter 27 1" src="/pages/1.jpg">
					<img class="imgholder" alt="Series Chapter 27 2" src="/pages/2.webp">
				</div>
			""".trimIndent(),
			"https://demonicscans.org/chaptered.php?chapter=27&manga=1572",
		)

		val pages = document.select(DEMONIC_PAGE_SELECTOR).map { it.requireSrc() }

		assertEquals(
			listOf(
				"https://demonicscans.org/pages/1.jpg",
				"https://demonicscans.org/pages/2.webp",
			),
			pages,
		)
	}
}

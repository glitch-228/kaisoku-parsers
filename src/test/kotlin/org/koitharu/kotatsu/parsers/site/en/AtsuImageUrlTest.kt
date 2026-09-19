package org.koitharu.kotatsu.parsers.site.en

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AtsuImageUrlTest {

	@Test
	fun `rewrites legacy Atsu image host to CDN`() {
		assertEquals(
			"https://cdn.atsu.moe/static/pages/example.avif",
			toAtsuCdnUrl("https://atsu.moe/static/pages/example.avif"),
		)
	}

	@Test
	fun `keeps CDN image host`() {
		assertEquals(
			"https://cdn.atsu.moe/static/pages/example.avif",
			toAtsuCdnUrl("https://cdn.atsu.moe/static/pages/example.avif"),
		)
	}
}

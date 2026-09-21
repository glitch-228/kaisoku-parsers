package org.koitharu.kotatsu.parsers.site.en

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AtsuImageUrlTest {

	@Test
	fun `preserves external image hosts and signed query strings`() {
		val url = "https://images.example.org/page.avif?signature=a%2Fb%2Bc"
		assertEquals(url, toAtsuCdnUrl(url))
	}

	@Test
	fun `preserves encoded paths and queries on migrated URLs`() {
		assertEquals(
			"https://cdn.atsu.moe/static/a%20b.avif?signature=a%2Fb%2Bc",
			toAtsuCdnUrl("https://atsu.moe/static/a%20b.avif?signature=a%2Fb%2Bc"),
		)
	}

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

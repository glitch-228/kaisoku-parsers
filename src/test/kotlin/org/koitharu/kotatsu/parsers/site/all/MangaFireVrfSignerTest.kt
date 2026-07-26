package org.koitharu.kotatsu.parsers.site.all

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Pins the `vrf` signature against vectors captured from live requests that MangaFire accepted with
 * HTTP 200 — an unsigned or wrongly-signed request answers `403 {"message":"Missing token."}`.
 */
internal class MangaFireVrfSignerTest {

	@Test
	fun `canonical form drops the api prefix and sorts parameters`() {
		assertEquals(
			"/titles?limit=3&order[chapter_updated_at]=desc&page=1",
			MangaFireVrfSigner.canonicalize(
				"/api/titles",
				listOf("limit" to "3", "order[chapter_updated_at]" to "desc", "page" to "1"),
			),
		)
	}

	@Test
	fun `repeated bracket parameters are indexed in order`() {
		assertEquals(
			"/titles?language[0]=en&language[1]=fr&page=1",
			MangaFireVrfSigner.canonicalize(
				"/api/titles",
				listOf("language[]" to "en", "language[]" to "fr", "page" to "1"),
			),
		)
	}

	@Test
	fun `path without parameters keeps no query`() {
		assertEquals("/titles/2pvv5", MangaFireVrfSigner.canonicalize("/api/titles/2pvv5", emptyList()))
	}

	@Test
	fun `signature matches vectors accepted by the live api`() {
		assertEquals("8sK3xtqdFZdD1d-yEmmI1uMCFYnFaw", MangaFireVrfSigner.sign("/titles?limit=2&page=1"))
		assertEquals(
			"8sK3xtqdFZdD1d-yEmkXNCZw00DrNDZc5m1UJcuor3W0juhfWQ7jm9RyzHx1cSmHSPUWMuI",
			MangaFireVrfSigner.sign("/titles?limit=3&order[chapter_updated_at]=desc&page=1"),
		)
		assertEquals("8sK3xtqdFdvBEwd9mQ", MangaFireVrfSigner.sign("/titles/2pvv5"))
		assertEquals("8vPRXa1JjvVTxnmIcFtgxrE", MangaFireVrfSigner.sign("/chapters/9317020"))
		assertEquals(
			"8sK3xtqdFZdDWTKuDVd13ugW71l3hZbPAc88R3tnTtqal4T56e4Q8AOrB-E",
			MangaFireVrfSigner.sign("/titles?language[0]=en&language[1]=fr&page=1"),
		)
	}
}

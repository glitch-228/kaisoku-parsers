package org.koitharu.kotatsu.parsers.site.madara.pt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.MangaLoaderContextMock
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.SortOrder

internal class YaoiX3Test {

	private val parser = YaoiX3(MangaLoaderContextMock)

	@Test
	fun `uses non ajax browse and pagination routes`() {
		assertEquals(
			"https://3xyaoi.com/bl/?m_orderby=latest",
			buildYaoiX3ListUrl("3xyaoi.com", 0, SortOrder.UPDATED, MangaListFilter.EMPTY),
		)
		assertEquals(
			"https://3xyaoi.com/bl/page/2/?m_orderby=views",
			buildYaoiX3ListUrl("3xyaoi.com", 1, SortOrder.POPULARITY, MangaListFilter.EMPTY),
		)
	}

	@Test
	fun `uses current search and archive routes`() {
		assertEquals(
			"https://3xyaoi.com/?s=demo+title&post_type=wp-manga",
			buildYaoiX3ListUrl(
				"3xyaoi.com",
				0,
				SortOrder.RELEVANCE,
				MangaListFilter(query = " demo title "),
			),
		)
		assertEquals(
			"https://3xyaoi.com/end/?m_orderby=latest",
			buildYaoiX3ListUrl(
				"3xyaoi.com",
				0,
				SortOrder.UPDATED,
				MangaListFilter(states = setOf(MangaState.FINISHED)),
			),
		)
		assertEquals(
			"https://3xyaoi.com/genero/romance/?m_orderby=latest",
			buildYaoiX3ListUrl(
				"3xyaoi.com",
				0,
				SortOrder.UPDATED,
				MangaListFilter(
					tags = setOf(MangaTag(title = "Romance", key = "romance", source = parser.source)),
				),
			),
		)
	}

	@Test
	fun `uses browser navigation headers required by the site`() {
		val headers = parser.getRequestHeaders()

		assertEquals("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", headers["Accept"])
		assertEquals("navigate", headers["Sec-Fetch-Mode"])
		assertEquals("document", headers["Sec-Fetch-Dest"])
		assertEquals("none", headers["Sec-Fetch-Site"])
		assertEquals("?1", headers["Sec-Fetch-User"])
		assertEquals("no-cache", headers["Pragma"])
	}
}

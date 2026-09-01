package org.koitharu.kotatsu.parsers.util

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.MangaLoaderContextMock
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import kotlin.time.Duration.Companion.minutes

internal class LinkResolverTest {

	private val context = MangaLoaderContextMock

	@Test
	@Disabled("The public fixture URL is intentionally redacted")
	fun supportedSource() = runTest(timeout = 2.minutes) {
		val resolver = context.newLinkResolver("REDACTED" /* do not publish links to manga on GitHub */)
		Assertions.assertEquals(MangaParserSource.MANGADEX, resolver.getSource())
		val manga = resolver.getManga()
		Assertions.assertEquals(resolver.link.toString(), manga?.publicUrl)
	}

	@Test
	@Disabled("The public fixture URL is intentionally redacted")
	fun unsupportedSource2() = runTest(timeout = 2.minutes) {
		val resolver = context.newLinkResolver("REDACTED" /* do not publish links to manga on GitHub */)
		Assertions.assertEquals(MangaParserSource.XBATCAT, resolver.getSource())
		val manga = resolver.getManga()
		Assertions.assertEquals(resolver.link.toString(), manga?.publicUrl)
	}
}

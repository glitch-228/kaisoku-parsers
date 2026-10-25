package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.en.MangagoParser
import org.koitharu.kotatsu.parsers.util.generateUid

internal class MangagoReleasedListingTest {
    @Test fun `only the identified deleted-upload bin is omitted`() = runTest {
        val parser = MangagoParser(Context(row("DELETE", "/read-manga/delete_1/") + row("Valid", "/read-manga/valid/")))
        val entries = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter())
        assertEquals(listOf("Valid"), entries.map { it.title })
        assertEquals(parser.generateUid("/read-manga/valid/"), entries.single().id)
    }

    @Test fun `title similarity alone never hides a real work`() = runTest {
        val parser = MangagoParser(Context(row("DELETE", "/read-manga/another_work/") +
            row("Actual title", "/read-manga/delete_9/") + row("Delete", "/read-manga/delete_3/")))
        val entries = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter())
        assertEquals(listOf("DELETE", "Actual title", "Delete"), entries.map { it.title })
        assertEquals(entries.map { parser.generateUid(it.url) }, entries.map { it.id })
    }

    @Test fun `valid listing order identities and public URLs remain unchanged`() = runTest {
        val parser = MangagoParser(Context(row("First", "/read-manga/first/") + row("Last", "/read-manga/last/")))
        val entries = parser.getListPage(1, SortOrder.UPDATED, MangaListFilter())
        assertEquals(listOf("/read-manga/first/", "/read-manga/last/"), entries.map { it.url })
        assertEquals(entries.map { "https://www.mangago.me${it.url}" }, entries.map { it.publicUrl })
    }

    private fun row(title: String, path: String) = """<div class='updatesli'><a class='thm-effect' title='$title' href='$path'><img src='https://cdn.test/cover.jpg'></a></div>"""
    private class Context(private val html: String) : MangaLoaderContext() {
        override val cookieJar = InMemoryCookieJar()
        override val httpClient = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(html.toResponseBody()).build()
        }.build()
        override fun getConfig(source: MangaSource) = SourceConfigMock()
        override fun getDefaultUserAgent() = "fixture"
        override suspend fun evaluateJs(script: String): String? = null
        override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null
        override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response = error("unused")
        override fun createBitmap(width: Int, height: Int): Bitmap = error("unused")
    }
}

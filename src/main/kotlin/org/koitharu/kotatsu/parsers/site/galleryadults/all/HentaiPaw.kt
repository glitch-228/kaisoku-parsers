package org.koitharu.kotatsu.parsers.site.galleryadults.all

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.model.SortOrder
import org.koitharu.kotatsu.parsers.util.generateUid
import org.koitharu.kotatsu.parsers.util.json.nextJsObjects
import org.koitharu.kotatsu.parsers.util.parseHtml
import org.koitharu.kotatsu.parsers.util.src
import org.koitharu.kotatsu.parsers.util.toAbsoluteUrl

/** The public gallery and reader expose all image URLs without executing site scripts. */
@MangaSourceParser("HENTAIPAW", "HentaiPaw", type = ContentType.HENTAI)
internal class HentaiPaw(context: MangaLoaderContext) :
    PagedMangaParser(context, MangaParserSource.HENTAIPAW, 24) {

    override val configKeyDomain = ConfigKey.Domain("hentaipaw.com")
    override val availableSortOrders = setOf(SortOrder.UPDATED, SortOrder.POPULARITY)
    override val filterCapabilities = MangaListFilterCapabilities(isSearchSupported = true)

    override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
        .set("Referer", "https://$domain/").build()

    override suspend fun getFilterOptions() = MangaListFilterOptions()

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = "https://$domain/".toHttpUrl().newBuilder()
        when {
            !filter.query.isNullOrBlank() -> url.addPathSegments("articles/search")
                .addQueryParameter("keyword", filter.query)
            order == SortOrder.POPULARITY -> url.addPathSegments("articles/rank")
        }
        url.addQueryParameter("page", page.toString())
        val doc = webClient.httpGet(url.build()).parseHtml()
        return doc.select("main a[href]").mapNotNull { anchor ->
            val href = anchor.attr("href").toAbsoluteUrl(domain).toHttpUrl()
            if (href.host != domain || !GALLERY_PATH.matches(href.encodedPath)) return@mapNotNull null
            val image = anchor.selectFirst("img") ?: return@mapNotNull null
            val title = image.attr("alt").trim().ifEmpty {
                anchor.attr("title").trim().ifEmpty { anchor.text().trim() }
            }
            if (title.isBlank()) return@mapNotNull null
            val path = href.encodedPath.removeSuffix("/")
            Manga(
                id = generateUid(path), url = path, publicUrl = "https://$domain$path", title = title,
                altTitles = emptySet(), coverUrl = image.src()?.toAbsoluteUrl(domain), rating = RATING_UNKNOWN,
                authors = emptySet(), tags = emptySet(), state = null, contentRating = ContentRating.ADULT,
                source = source,
            )
        }.distinctBy { it.id }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
        val details = doc.selectFirst("#article-details")
            ?: throw ParseException("Gallery details not found", doc.location())
        val viewer = details.selectFirst("#button-group a[href*=/viewer?]")?.attr("href")
            ?.toAbsoluteUrl(domain)?.toHttpUrl()
            ?: throw ParseException("Gallery reader link not found", doc.location())
        if (viewer.host != domain || viewer.encodedPath != "/viewer" ||
            viewer.queryParameter("articleId") != manga.url.trimEnd('/').substringAfterLast('/')) {
            throw ParseException("Gallery reader identity does not match", doc.location())
        }
        val chapterUrl = viewer.encodedPath + "?" + viewer.encodedQuery
        val title = details.selectFirst("h1")?.text()?.takeIf(String::isNotBlank) ?: manga.title
        val tags = details.select("#article-tag-information a[href^=/tags/]").map { tag ->
            MangaTag(key = tag.attr("href"), title = tag.text(), source = source)
        }.toSet()
        return manga.copy(
            title = title,
            coverUrl = details.selectFirst("img")?.src()?.toAbsoluteUrl(domain) ?: manga.coverUrl,
            authors = details.select("#article-tag-information a[href^=/artists/]").map { it.text() }.toSet(),
            tags = tags,
            contentRating = ContentRating.ADULT,
            chapters = listOf(MangaChapter(
                id = generateUid(chapterUrl), url = chapterUrl, title = title, number = 1f, volume = 0,
                uploadDate = 0, branch = null, scanlator = null, source = source,
            )),
        )
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
        val urls = readerUrls(doc)
        if (urls.isEmpty()) throw ParseException("Gallery image URLs not found", doc.location())
        return urls.map { MangaPage(id = generateUid(it), url = it, preview = null, source = source) }
    }

    private fun readerUrls(doc: Document): List<String> {
        val slides = doc.nextJsObjects().firstNotNullOfOrNull { it.optJSONArray("slides") }
            ?: return emptyList()
        if (slides.length() > MAX_PAGES) throw ParseException("Gallery has too many pages", doc.location())
        return (0 until slides.length()).map { index ->
            val value = slides.getJSONObject(index).getString("src").toAbsoluteUrl(domain)
            val url = value.toHttpUrl()
            if (!url.isHttps) throw ParseException("Gallery image must use HTTPS", doc.location())
            url.toString()
        }
    }

    private companion object {
        val GALLERY_PATH = Regex("""/articles/\d+/?""")
        const val MAX_PAGES = 5_000
    }
}

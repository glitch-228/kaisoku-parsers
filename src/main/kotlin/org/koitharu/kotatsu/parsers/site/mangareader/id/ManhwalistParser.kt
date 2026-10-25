package org.koitharu.kotatsu.parsers.site.mangareader.id

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser
import java.util.*
import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.parsers.config.ConfigKey

@MangaSourceParser("MANHWALIST", "ManhwaList", "id")
internal class ManhwalistParser(context: MangaLoaderContext) :
    MangaReaderParser(context, MangaParserSource.MANHWALIST, "m1.manhwalist.my.id", pageSize = 24, searchPageSize = 10) {
    override val configKeyDomain = ConfigKey.Domain("m1.manhwalist.my.id", "manhwalist.xyz")

    private val siteDomain get() = if (domain in RETIRED_HOSTS) "m1.manhwalist.my.id" else domain

    override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
        .set("Referer", "https://$siteDomain/").build()

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.url.host !in RETIRED_HOSTS) return super.intercept(chain)
        val request = original.newBuilder().url(original.url.newBuilder().scheme("https").host(siteDomain).build())
            .header("Referer", "https://$siteDomain/").build()
        return super.intercept(object : Interceptor.Chain by chain {
            override fun request() = request
        })
    }

    private companion object {
        val RETIRED_HOSTS = setOf("manhwalist.xyz", "www.manhwalist.xyz")
    }

    override val sourceLocale: Locale = Locale.ENGLISH
}

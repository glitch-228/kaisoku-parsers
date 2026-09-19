package org.koitharu.kotatsu.parsers.site.en

import okhttp3.HttpUrl.Companion.toHttpUrl

internal fun toAtsuCdnUrl(url: String): String {
	val host = url.toHttpUrl().host
	return if (host.startsWith("cdn.")) url else url.replaceFirst(PROTOCOL_REGEX, "https://cdn.")
}

private val PROTOCOL_REGEX = Regex("^https?:?//")

package org.koitharu.kotatsu.parsers.site.en

import okhttp3.HttpUrl.Companion.toHttpUrl

internal fun toAtsuCdnUrl(url: String): String {
	val parsed = url.toHttpUrl()
	return if (parsed.host == "atsu.moe") {
		parsed.newBuilder().scheme("https").host("cdn.atsu.moe").build().toString()
	} else {
		url
	}
}

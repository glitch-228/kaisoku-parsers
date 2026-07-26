package org.koitharu.kotatsu.parsers.site.mangareader.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser

/**
 * The group rebranded from ErosScans to Scythe Scans: `erosxscans.xyz` now serves an empty parked
 * page and `erosscans.com` no longer resolves. The source id stays `EROSSCANS` so existing
 * favourites and history keep resolving. Its reader ships the `ts_reader.run` config inside a base64
 * `data:text/javascript` script, which is what [encodedSrc] handles.
 */
@MangaSourceParser("EROSSCANS", "Scythe Scans", "en")
internal class ErosScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.EROSSCANS, "scythescans.com", pageSize = 20, searchPageSize = 10) {

	override val encodedSrc = true
}

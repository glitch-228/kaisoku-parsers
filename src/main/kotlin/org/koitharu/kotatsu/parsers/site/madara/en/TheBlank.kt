package org.koitharu.kotatsu.parsers.site.madara.en

import org.koitharu.kotatsu.parsers.Broken
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.madara.MadaraParser

/**
 * theblank.net left WordPress/Madara for a Laravel + Inertia.js app, so every endpoint below is gone:
 * page state now ships as JSON in `<div id="app" data-page="...">`, series live at `/serie/<slug>` and
 * chapters at `/serie/<slug>/chapter/<uid>-chapter-<n>`.
 *
 * Rewriting the browse/details half would be easy — that JSON is clean — but the reader cannot be
 * followed: a chapter page carries no image URLs at all, only `page_count`, a `chapter_token`, an
 * `attestation` bound to a WebGL seed, an X25519 `server_pubkey` and a wrapped content key. Pages are
 * fetched encrypted, decrypted by a WASM module (`/wasm/pam.js`) and painted into `<canvas>` elements.
 * There is no URL for a parser to return, so this source has no feasible read path.
 */
@Broken("Encrypted WASM/canvas reader with WebGL attestation; no feasible read path")
@MangaSourceParser("THEBLANK", "TheBlank", "en", ContentType.HENTAI)
internal class TheBlank(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.THEBLANK, "theblank.net") {
	override val datePattern = "dd/MM/yyyy"
}

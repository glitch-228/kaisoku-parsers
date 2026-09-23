package org.koitharu.kotatsu.parsers.site.ru

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.exception.ParseException

/** Map the September 2026 TanStack loader data onto the existing parser fields. */
internal suspend fun parseInkStoryState(context: MangaLoaderContext, document: Document, url: String): Map<*, *>? {
    val state = document.getElementById("\$tsr-stream-barrier")?.data() ?: return null
    val result = context.evaluateJs("", inkStoryStateScript(state))
        ?: throw ParseException("Cannot decode InkStory page state", url)
    // Android WebView quotes string results; host evaluators may return the string directly.
    val decoded = JSONTokener(result).nextValue()
    val json = if (decoded is String) JSONObject(decoded) else decoded as? JSONObject
        ?: throw ParseException("Invalid InkStory page state", url)
    return jsonValueToCollections(json) as Map<*, *>
}

internal fun inkStoryStateScript(state: String): String = """
    (function() {
        var ${'$'}R, ${'$'}_TSR;
        var document = {currentScript: {remove: function() {}}};
        // Deferred query streams are not needed: the complete loader data is in matches.
        var ReadableStream = function() {};
        ${state.replace("self.\$R", "\$R").replace("self.\$_TSR", "\$_TSR")}
        var data = {};
        if (!${'$'}_TSR || !${'$'}_TSR.router) throw new Error('Missing InkStory router state');
        ${'$'}_TSR.router.matches.forEach(function(match) {
            var before = match.b || {}, loader = match.l || {};
            if (before.root) {
                data['secret-key'] = before.root.decryptionKey;
                data.session = {currentUser: before.root.currentUser};
            }
            if (before.book || loader.book) data['current-book'] = before.book || loader.book;
            if (loader.books) data['catalog-books'] = loader.books;
            if (loader.labels) data['catalog-labels'] = loader.labels;
            if (loader.branches) data['current-book-branches'] = loader.branches;
            if (loader.chapters) data['current-book-chapters'] = loader.chapters;
            if (loader.initialChapter) data['reader-current-chapter'] = loader.initialChapter;
        });
        return JSON.stringify(data);
    })();
""".trimIndent()

private fun jsonValueToCollections(value: Any?): Any? = when (value) {
    null, JSONObject.NULL -> null
    is JSONObject -> value.keys().asSequence().associateWith { jsonValueToCollections(value.get(it)) }
    is JSONArray -> (0 until value.length()).map { jsonValueToCollections(value.get(it)) }
    else -> value
}

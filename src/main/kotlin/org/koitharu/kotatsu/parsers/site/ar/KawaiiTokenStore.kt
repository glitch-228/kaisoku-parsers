package org.koitharu.kotatsu.parsers.site.ar

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** An anonymous API session, never an account credential or a persisted source setting. */
internal class KawaiiTokenStore(private val now: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private val mutex = Mutex()
    private var value: String? = null
    private var expiresAt = 0L

    suspend fun get(load: suspend () -> Pair<String, Long>): String = mutex.withLock {
        val cached = value
        if (cached != null && now() < expiresAt) return@withLock cached
        val requestedAt = now()
        val (token, seconds) = load()
        require(token.isNotBlank() && token.length <= 16_384) { "Invalid anonymous API token" }
        val lifetime = seconds.coerceIn(0, 86_400) * 1_000
        val earlyRefresh = minOf(120_000, lifetime / 10)
        value = token
        expiresAt = requestedAt + lifetime - earlyRefresh
        token
    }

    suspend fun invalidate(used: String) = mutex.withLock {
        // A late failure must not discard a newer session established by another request.
        if (value == used) {
            value = null
            expiresAt = 0
        }
    }
}

package org.koitharu.kotatsu.parsers.util

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import kotlin.coroutines.resumeWithException

internal class ContinuationCallCallback(
    call: Call,
    continuation: CancellableContinuation<Response>,
) : Callback, CompletionHandler {

    @Volatile
    private var call: Call? = call

    @Volatile
    private var continuation: CancellableContinuation<Response>? = continuation

    override fun onResponse(call: Call, response: Response) {
        val continuation = takeContinuation()
        if (continuation == null) {
            response.closeQuietly()
            return
        }
        continuation.resume(response) { _, value, _ ->
            value.closeQuietly()
        }
    }

    override fun onFailure(call: Call, e: IOException) {
        val continuation = takeContinuation() ?: return
        continuation.resumeWithException(e)
    }

    override fun invoke(cause: Throwable?) {
        val call = takeCall()
        runCatching {
            call?.cancel()
        }.onFailure { e ->
            cause?.addSuppressed(e)
        }
    }

    private fun takeContinuation(): CancellableContinuation<Response>? = synchronized(this) {
        continuation.also {
            continuation = null
            call = null
        }
    }

    private fun takeCall(): Call? = synchronized(this) {
        call.also {
            continuation = null
            call = null
        }
    }
}

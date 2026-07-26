package org.koitharu.kotatsu.parsers.site.all

import okhttp3.Interceptor
import okio.ByteString
import okio.ByteString.Companion.decodeBase64

/**
 * Signs MangaFire API requests. Every `/api/` route except a couple of open ones answers
 * `403 {"message":"Missing token."}` without a `vrf` query parameter.
 *
 * The signature is taken over a canonical form of the request: the path with its `/api` prefix
 * dropped, followed by the query parameters sorted by name, where repeated `key[]` parameters are
 * rewritten to `key[0]`, `key[1]`, … in order. That string is then run through three substitution
 * stages — each byte is XORed with a rotating key byte and the previous output byte, then mapped
 * through a 256-entry table — and emitted as unpadded base64url.
 */
internal class MangaFireVrfSigner {

	fun interceptor() = Interceptor { chain ->
		val request = chain.request()
		val url = request.url
		if (!url.encodedPath.startsWith("/api/")) {
			return@Interceptor chain.proceed(request)
		}
		val params = url.queryParameterNames
			.flatMap { name -> url.queryParameterValues(name).map { name to it } }
			.sortedBy { it.first }
		val builder = url.newBuilder().query(null)
		params.forEach { (name, value) -> builder.addQueryParameter(name, value) }
		builder.addQueryParameter("vrf", sign(canonicalize(url.encodedPath, params)))
		chain.proceed(request.newBuilder().url(builder.build()).build())
	}

	companion object {

		/** Builds the string the signature is computed over. Visible for testing. */
		internal fun canonicalize(encodedPath: String, params: List<Pair<String, String?>>): String = buildString {
			append(encodedPath.removePrefix("/api"))
			if (params.isEmpty()) {
				return@buildString
			}
			append('?')
			var lastName = ""
			var index = 0
			append(
				params.joinToString("&") { (name, value) ->
					val indexedName = if (name.endsWith("[]")) {
						if (lastName != name) index = 0
						lastName = name
						name.replace("[]", "[${index++}]")
					} else {
						name
					}
					"$indexedName=$value"
				},
			)
		}

		/** Visible for testing. */
		internal fun sign(canonical: String): String {
			var data = canonical.toByteArray(Charsets.UTF_8)
			for ((table, key, iv) in STAGES) {
				data = encryptStage(data, table, key, iv)
			}
			return ByteString.of(*data).base64Url().trimEnd('=')
		}

		private fun encryptStage(data: ByteArray, table: ByteArray, key: ByteArray, iv: Int): ByteArray {
			val out = ByteArray(data.size)
			var prev = iv
			for (i in data.indices) {
				prev = table[(data[i].toInt() xor key[i % key.size].toInt() xor prev) and 0xFF].toInt() and 0xFF
				out[i] = prev.toByte()
			}
			return out
		}

		private fun decodeB64(value: String) = checkNotNull(value.decodeBase64()).toByteArray()

		private const val TABLE_1 =
			"yINlmUNho8VYJT+ibTIP+9ESiULpVEtMOoD6U6lRE0R/xwXo/Xp9NrUgC4cw/Lmo33vUyjUE40kUoEWIr/fxfNNcq2s79ShQ5NhNrFnJ4hXPwOu/SuXzIbuTQKGFvfm08E9jvCfqAtoDqvQq3dVWPQFmJjgvkISBeXY3BgANR+yVnjGbcxZ47d6kLNfZPIayTq3/YGySb1KuVZodWp/WGNAO5pfMcpaK53Hhs0allBszaMaxuouOwdxbwgxIw6YunSsXjI05Yi0j9j4eHKfSXR8Ifo/Od+8iamRfCXTyvm7NGRGYdcQ0ywcK/u6RXhrbcCm4t2eCtrDgQVecJGkQ+A=="
		private const val KEY_1 = "0Ec58JOY3uBzJK9m3zqIOpdlF7UFiax9DmA="
		private const val TABLE_2 =
			"IUFltCxD3Oc2cwCgkJffthaOg9cgPUb0LgW6H/VtfcF0kc5F25t+aWj6JH9VOhOaY0rAFdUxlDnl5BLNvwEJvQtP5qcw7vdb/K+chnbwnspSHT8mz5lqwz41TezG0hkO06FTjJZhsyNuFLDpD2ZZxQj/QIRcF90zpmQ7Byu483WsQqUE0C342HL+JXngRB6fRzxRyVTaKu83h7UYTJ0QMt6ixFh6S3F8gqkKwrGTL3jHNBsD45UnifK8+RGtishQV2K3rujLKEkiZxpr2dYcudFW4oFsDKhad3CLBvuyTqsCo4B7mL5IKQ1vXo/MOOvq1I1d8ar9X6Ttu5KF4fZgiA=="
		private const val KEY_2 = "AAdjb1iPY8CiDmq9H34tKTBF8a3oDQ=="
		private const val TABLE_3 =
			"NQHlu1/wVO5EmkwQymF810qqY2xG1k2obcas4Z9mCsPEIFl9pRIjFxbJ7ybMHbBckT5Ton85E0FOeHezbh/mjlEYpmpnlXOS8dgrqeq2KfxImTh1YK9y0PeMNhzA1OQzSY9brYOJq/l2QnE/hwOeZIhPixVSKIUlDb5vLcH6RWKxkIEMuP0bDwIqQ71AJJaEaMJL7A6YtyIwoRT+L5v4aZzodN/0+3nOGsfblFjgxSfPzVDjNFeNl5P26+kEC/8AHgdrpAbt3hHz3HrRN1Y6e+JHgF7ncFWnoF0y3THL1S71WgWGCa6KtSzTCCG58n68nTyj2T3Sshk7utqCtMi/ZQ=="
		private const val KEY_3 = "DELOJgPsVaCcblDtTGMdHzM="

		private val STAGES: List<Triple<ByteArray, ByteArray, Int>> = listOf(
			Triple(decodeB64(TABLE_1), decodeB64(KEY_1), 0x5A),
			Triple(decodeB64(TABLE_2), decodeB64(KEY_2), 0x35),
			Triple(decodeB64(TABLE_3), decodeB64(KEY_3), 0xBA),
		)
	}
}

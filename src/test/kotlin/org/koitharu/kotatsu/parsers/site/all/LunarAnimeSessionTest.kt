package org.koitharu.kotatsu.parsers.site.all

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaLoaderContextMock
import org.koitharu.kotatsu.parsers.SourceConfigMock
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class LunarAnimeSessionTest {

	@Test fun `uses current proof header and preserves page identity`() = runTest {
		val context = FixtureContext()
		val pages = LunarAnime(context).getPages(chapter)
		assertEquals(listOf("https://api.lunarx.to/pages/1.jpg", "https://vault.lunarx.to/pages/2.jpg"), pages.map { it.url })
		assertEquals(2, pages.map { it.id }.distinct().size)
		assertTrue(pages.all { it.source == MangaParserSource.LUNARANIME })
		assertEquals(1, context.sessionRequests)
		assertEquals(pages.map { it.id }, LunarAnime(context).getPages(chapter).map { it.id })
	}

	@Test fun `decrypts session bound to existing browser public key`() = runTest {
		val context = FixtureContext(bindKey = true)
		assertEquals(2, LunarAnime(context).getPages(chapter).size)
	}

	@Test fun `placeholder pages request chapter validation instead of entering the reader`() = runTest {
		val context = FixtureContext(images = listOf("/api/cdn/p/one", "https://api.lunarx.to/api/cdn/p/two"))
		try {
			LunarAnime(context).getPages(chapter)
			fail<Unit>("Placeholder pages were accepted")
		} catch (e: ValidationRequired) {
			assertEquals("https://lunarx.to${chapter.url}", e.url)
		}
	}

	@Test fun `server revalidation metadata requests validation before decrypting`() = runTest {
		val context = FixtureContext(revalidation = true)
		try {
			LunarAnime(context).getPages(chapter)
			fail<Unit>("Revalidation response was accepted")
		} catch (e: ValidationRequired) {
			assertEquals("https://lunarx.to${chapter.url}", e.url)
		}
	}

	@Test fun `missing browser identity does not submit unsigned session request`() = runTest {
		val context = FixtureContext(hasProof = false)
		try {
			LunarAnime(context).getPages(chapter)
			fail<Unit>("Unsigned session request was accepted")
		} catch (_: ValidationRequired) {
			assertEquals(0, context.sessionRequests)
		}
	}

	@Test fun `cancellation of browser signing remains cancellation`() = runTest {
		val context = FixtureContext(cancel = true)
		try {
			LunarAnime(context).getPages(chapter)
			fail<Unit>("Cancellation was swallowed")
		} catch (_: CancellationException) {
			assertEquals(0, context.sessionRequests)
		}
	}

	@Test fun `recognizes validation states without treating every image as a placeholder`() {
		assertTrue(LunarReaderSession.requiresValidation(JSONObject("""{"slug":"unknown"}"""), emptyList()))
		assertTrue(LunarReaderSession.requiresValidation(JSONObject("""{"data":{"cache_status":"revalidate"}}"""), emptyList()))
		assertFalse(LunarReaderSession.requiresValidation(JSONObject(), listOf("https://vault.lunarx.to/pages/1.jpg")))
		assertFalse(LunarReaderSession.requiresValidation(JSONObject(), listOf("https://vault.lunarx.to/pages/1.jpg", "/api/cdn/p/two")))
	}

	private class ValidationRequired(val url: String) : RuntimeException()

	private class FixtureContext(
		val images: List<String> = listOf("/pages/1.jpg", "https://vault.lunarx.to/pages/2.jpg"),
		val hasProof: Boolean = true,
		val revalidation: Boolean = false,
		val bindKey: Boolean = false,
		val cancel: Boolean = false,
	) : MangaLoaderContext() {
		var sessionRequests = 0
		override val cookieJar = CookieJar.NO_COOKIES
		override val httpClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
			val request = chain.request()
			val content = if (request.url.host == "lunarx.to") {
				readerHtml()
			} else {
				sessionRequests++
				assertEquals("fixture-proof", request.header(LunarReaderSession.PROOF_HEADER))
				assertNull(request.header("dpop"))
				val nonce = decodeToken(request.url.pathSegments.last()).split('|')[1]
				val payload = JSONObject().put("slug", "fixture-title")
					.put("data", JSONObject().put("images", JSONArray(images)))
				var key = "$RCTX0\u0001$nonce"
				if (bindKey) {
					val canonical = """{"crv":"P-256","kty":"EC","x":"fixture-x","y":"fixture-y"}"""
					key += "\u0002" + Base64.getUrlEncoder().withoutPadding().encodeToString(sha(canonical))
				}
				val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
				cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(sha(key), "AES"), IvParameterSpec(ByteArray(16)))
				val encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(payload.toString().toByteArray()))
				JSONObject().put("data", JSONObject().put("session_data", encrypted))
					.apply { if (revalidation) put("cache_status", "revalidate") }.toString()
			}
			Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
				.body(content.toResponseBody("application/json".toMediaType())).build()
		}).build()

		override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? {
			if (cancel) throw CancellationException("Signing cancelled")
			assertEquals("https://lunarx.to/", baseUrl)
			val result = if (hasProof) JSONObject().put("proof", "fixture-proof").apply {
				if (bindKey) put("publicJwk", JSONObject().put("x", "fixture-x").put("y", "fixture-y"))
			} else JSONObject().put("validationRequired", true)
			return JSONObject.quote(result.toString())
		}
		@Deprecated("Provide a base url")
		override suspend fun evaluateJs(script: String): String? = error("Unexpected script")
		override fun requestBrowserAction(parser: org.koitharu.kotatsu.parsers.MangaParser, url: String): Nothing =
			throw ValidationRequired(url)
		override fun getConfig(source: MangaSource): MangaSourceConfig = SourceConfigMock()
		override fun getDefaultUserAgent() = "Fixture"
		override fun redrawImageResponse(response: Response, redraw: (Bitmap) -> Bitmap): Response =
			MangaLoaderContextMock.redrawImageResponse(response, redraw)
		override fun createBitmap(width: Int, height: Int): Bitmap = MangaLoaderContextMock.createBitmap(width, height)
	}

	private companion object {
		const val RCTX0 = "fixture-reader-context"
		const val RCTX1 = "fixture-second-context"
		val chapter = MangaChapter(5L, "Chapter 1", 1f, 0, "/manga/fixture-title/1?lang=en", null, 0L, "English", MangaParserSource.LUNARANIME)
		fun sha(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())

		fun readerHtml(): String {
			val raw = byteArrayOf(167.toByte(), 62, 145.toByte(), 0, RCTX0.length.toByte(), 0, RCTX1.length.toByte()) +
				RCTX0.toByteArray() + RCTX1.toByteArray()
			var previous = 0
			val packed = raw.joinToString("") { byte ->
				previous = (byte.toInt() and 255) xor previous
				"%02x".format(previous)
			}
			val descriptor = "3|07|01|01|000|chunk"
			val encoded = descriptor.mapIndexed { index, c -> (c.code xor ((101 + 37 * index) and 255)).toByte() }.toByteArray()
			val dictionary = JSONObject().put("e", Base64.getEncoder().encodeToString(encoded).reversed()).put("chunk", packed)
			return "<script>self.__next_f.push([1,${JSONObject.quote(dictionary.toString())}])</script>"
		}

		fun decodeToken(token: String): String {
			val encrypted = Base64.getUrlDecoder().decode(token)
			val digest = sha("$RCTX0\u0001$RCTX1")
			val key = ByteArray(maxOf(RCTX0.length, RCTX1.length)) { index ->
				(RCTX0[index % RCTX0.length].code xor RCTX1[index % RCTX1.length].code xor
					(digest[index % 32].toInt() and 255) xor ((83 * index + 29) and 255)).toByte()
			}
			val offset = encrypted.first().toInt() and 255
			return String(ByteArray(encrypted.size - 1) { index ->
				(encrypted[index + 1].toInt() xor key[(index + offset) % key.size].toInt() xor
					((offset + 83 * index) and 255)).toByte()
			})
		}
	}
}

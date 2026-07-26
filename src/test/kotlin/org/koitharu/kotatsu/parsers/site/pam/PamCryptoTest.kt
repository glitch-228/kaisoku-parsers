package org.koitharu.kotatsu.parsers.site.pam

import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.util.secretstream.X25519
import java.io.IOException
import java.util.Base64

/**
 * Covers the page pipeline of [PamParser], which cannot be checked against the live site from CI.
 *
 * The secretstream vectors were produced by libsodium itself (`crypto_secretstream_xchacha20poly1305_push`),
 * so a decryption regression fails here rather than silently serving corrupt images, and the X25519
 * vectors are the ones from RFC 7748 §6.1.
 */
internal class PamCryptoTest {

	@Test
	fun `x25519 matches the RFC 7748 vectors`() {
		val alicePrivate = "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a".decodeHex()
		val bobPublic = "de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f".decodeHex()
		assertEquals(
			"8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a",
			X25519.publicKey(alicePrivate).toHex(),
		)
		assertEquals(
			"4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742",
			X25519.scalarMult(alicePrivate, bobPublic).toHex(),
		)
	}

	@Test
	fun `secretstream decrypts a multi-chunk libsodium stream`() {
		val decrypted = decrypt(CIPHERTEXT_B64, chunkSize = CHUNK + ABYTES)
		assertArrayEquals(Base64.getDecoder().decode(PLAINTEXT_B64), decrypted)
	}

	@Test
	fun `secretstream reassembles chunks split across reads`() {
		// The network hands okio arbitrary fragments, so a chunk is rarely available in one read.
		val upstream = DribblingSource(Base64.getDecoder().decode(CIPHERTEXT_B64), bytesPerRead = 7)
		val decrypted = PamParser.SecretStreamSource(
			upstream = upstream.buffer(),
			header = Base64.getDecoder().decode(HEADER_B64),
			streamKey = Base64.getDecoder().decode(KEY_B64),
			chunkSize = CHUNK + ABYTES,
		).buffer().readByteArray()
		assertArrayEquals(Base64.getDecoder().decode(PLAINTEXT_B64), decrypted)
	}

	@Test
	fun `secretstream rejects a tampered chunk`() {
		val tampered = Base64.getDecoder().decode(CIPHERTEXT_B64).also { it[5] = (it[5] + 1).toByte() }
		assertThrows(IOException::class.java) {
			decrypt(Base64.getEncoder().encodeToString(tampered), chunkSize = CHUNK + ABYTES)
		}
	}

	@Test
	fun `stream key is the page digest xored with the key hint`() {
		val streamKey = PamParser.deriveStreamKey(
			sharedSecret = "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742".decodeHex(),
			pageName = "ZXRgg6Fulkni-001.webp",
			keyHint = Base64.getDecoder().decode("AQQHCg0QExYZHB8iJSgrLjE0Nzo9QENGSUxPUlVYW14="),
		)
		assertEquals("eb856568eb5a3b91e245248f8e6a356d991b8fe74de4eedfec2928e81a0d0533", streamKey.toHex())
	}

	@Test
	fun `page signature is hmac of index timestamp and nonce`() {
		assertEquals(
			"863d0c61766915cfda73f1341311512621f68c41e434027e63ce268c636bdd61",
			PamParser.sign(
				key = "3b2a48bc20009f548f458400ba90eedb17bdafe219aad75125b6fd9a1fd97fd3",
				message = PAGE_INDEX + TIMESTAMP + NONCE,
			),
		)
	}

	private fun decrypt(ciphertextB64: String, chunkSize: Long): ByteArray {
		val upstream = Buffer().write(Base64.getDecoder().decode(ciphertextB64))
		return PamParser.SecretStreamSource(
			upstream = upstream,
			header = Base64.getDecoder().decode(HEADER_B64),
			streamKey = Base64.getDecoder().decode(KEY_B64),
			chunkSize = chunkSize,
		).buffer().readByteArray()
	}

	/** Hands out at most [bytesPerRead] bytes at a time, the way a socket does. */
	private class DribblingSource(data: ByteArray, private val bytesPerRead: Long) : Source {

		private val remaining = Buffer().write(data)

		override fun read(sink: Buffer, byteCount: Long): Long = if (remaining.size == 0L) {
			-1L
		} else {
			remaining.read(sink, minOf(byteCount, bytesPerRead))
		}

		override fun timeout(): Timeout = Timeout.NONE

		override fun close() = Unit
	}

	private fun String.decodeHex() = ByteArray(length / 2) {
		substring(it * 2, it * 2 + 2).toInt(16).toByte()
	}

	private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

	private companion object {

		const val CHUNK = 64L
		const val ABYTES = 17L

		const val PAGE_INDEX = "1"
		const val TIMESTAMP = "1753720000"
		const val NONCE = "4deadbeef"

		const val KEY_B64 = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
		const val HEADER_B64 = "rflgwiDpPmWlIK+HDUmjcrXZ5ivTGYfo"

		/** Three full 64-byte messages plus a short final one, pushed by libsodium. */
		const val CIPHERTEXT_B64 = "82sHuUmP3miIINBZYUou5FsLsJRZharse+5LC43EQBxRMYrPJvsiyFuX9Mr7Mh2mn3IZNygoltX4" +
			"vuRUxVu27T0MgcWhOpUYs6IjvogDLtzIiVn1JjGSR5l9084+Bc9arU3DD9aJnRgaUSqejERISHnC+h5ywj0VUVi68LWNGf2A" +
			"MajEc8tnAYhxcrHNEvhAe9+hMK+fanYxXycUfv4fOOAnFFfRNFVWIZxOJA/cNM3E9+FebIideuSyJqXPvAFtLUAbFtTKFWmA" +
			"rqKQJq6if93x+xbNBvTV4KETYfpul+9atdQeXwWiStMWerpHtV6zJwT0uPZnasxVE+IQ3eJQlH9bRnZPkMdafR+H5NItv3xz" +
			"wfqBbg=="

		const val PLAINTEXT_B64 = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4" +
			"OTo7PD0+PwcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUYO" +
			"DxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNdGFpbC1vZi10" +
			"aGUtaW1hZ2U="
	}
}

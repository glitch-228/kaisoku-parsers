package org.koitharu.kotatsu.parsers.site.vi

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class MoeTruyenImageDecryptorTest {

	@TempDir
	lateinit var temporaryDirectory: Path

	@Test
	fun `decrypts IMGX v3 AES GCM payload`() {
		val storageKey = "/chapters/42/page-1.webp"
		val imageId = "page-1"
		val width = 1200
		val height = 1800
		val contentKey = ByteArray(32) { (it * 7 + 3).toByte() }
		val iv = ByteArray(12) { (it * 11 + 1).toByte() }
		val plaintext = "fixture-webp-payload".toByteArray()
		val grant = ImgxGrant(
			version = 3,
			algorithm = "AES-256-GCM",
			decodeKey = null,
			wrappedDecodeKey = null,
			wrappedContentKey = wrapContentKey(
				contentKey = contentKey,
				version = 3,
				algorithm = "AES-256-GCM",
				imageId = imageId,
				issuedAt = 100L,
				expiresAt = 200L,
				nonce = "nonce",
				keyNonce = "key-nonce",
				signature = "signature",
				storageKey = storageKey,
			),
			imageId = imageId,
			issuedAt = 100L,
			expiresAt = 200L,
			nonce = "nonce",
			keyNonce = "key-nonce",
			signature = "signature",
		)

		val aad = "IMGX-v3.$imageId.${storageKey.trimStart('/')}.$width.$height".toByteArray()
		val encrypted = Cipher.getInstance("AES/GCM/NoPadding").run {
			init(Cipher.ENCRYPT_MODE, SecretKeySpec(contentKey, "AES"), GCMParameterSpec(128, iv))
			updateAAD(aad)
			doFinal(plaintext)
		}
		val payload = ByteBuffer.allocate(25 + encrypted.size)
			.put("IMGX".toByteArray())
			.put(3)
			.putInt(width)
			.putInt(height)
			.put(iv)
			.put(encrypted)
			.array()

		val result = ImageDecryptor.decrypt(payload, grant, storageKey)

		assertArrayEquals(plaintext, result.data.copyOfRange(result.offset, result.offset + result.size))
	}

	@Test
	fun `cache pruning removes stale and excess files but keeps active pages`() {
		val now = 10_000L
		val directory = temporaryDirectory.toFile()
		val stale = directory.resolve("stale.webp").apply {
			writeBytes(ByteArray(4))
			setLastModified(1_000L)
		}
		val old = directory.resolve("old.webp").apply {
			writeBytes(ByteArray(8))
			setLastModified(7_000L)
		}
		val active = directory.resolve("active.webp").apply {
			writeBytes(ByteArray(8))
			setLastModified(9_900L)
		}
		val partial = directory.resolve("page.webp.part").apply { writeBytes(ByteArray(4)) }

		pruneMoeTruyenCache(
			directory = directory,
			now = now,
			maxBytes = 8L,
			maxAgeMs = 5_000L,
			activeGraceMs = 500L,
		)

		assertFalse(stale.exists())
		assertFalse(old.exists())
		assertFalse(partial.exists())
		assertTrue(active.exists())
	}

	private fun wrapContentKey(
		contentKey: ByteArray,
		version: Int,
		algorithm: String,
		imageId: String,
		issuedAt: Long,
		expiresAt: Long,
		nonce: String,
		keyNonce: String,
		signature: String,
		storageKey: String,
	): String {
		val grantString = listOf(
			"IMGX-GRANT-WRAP-v1",
			version.toString(),
			algorithm,
			imageId,
			issuedAt.toString(),
			expiresAt.toString(),
			nonce,
			keyNonce,
			signature,
			storageKey.trimStart('/'),
		).joinToString(".")
		val wrappingKey = deriveKey(grantString)
		val wrapped = ByteArray(contentKey.size) { i ->
			(contentKey[i].toInt() xor wrappingKey[i].toInt()).toByte()
		}
		return Base64.getUrlEncoder().withoutPadding().encodeToString(wrapped)
	}

	private fun deriveKey(input: String): ByteArray {
		val result = ByteArray(32)
		var hash = fnv1a(input.toByteArray())
		for (i in result.indices) {
			if (i % 4 == 0) hash = xorshift32(hash + i + GOLDEN_RATIO)
			result[i] = (hash ushr ((i % 4) * 8) and 0xff).toByte()
		}
		return result
	}

	private fun fnv1a(data: ByteArray): Int {
		var hash = 2166136261.toInt()
		for (byte in data) hash = (hash xor (byte.toInt() and 0xff)) * 16777619
		return if (hash == 0) GOLDEN_RATIO else hash
	}

	private fun xorshift32(input: Int): Int {
		var value = input
		value = value xor (value shl 13)
		value = value xor (value ushr 17)
		value = value xor (value shl 5)
		return value
	}

	private companion object {
		const val GOLDEN_RATIO = -1640531527
	}
}

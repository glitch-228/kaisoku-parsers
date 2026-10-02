package org.koitharu.kotatsu.parsers.site.all

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

internal object LunarReaderSession {

	const val PROOF_HEADER = "cant-catch-this-monkey"

	fun requiresValidation(payload: JSONObject, images: List<String>): Boolean =
		payload.optString("slug") == "unknown" ||
			payload.optString("cache_status") == "revalidate" ||
			payload.optJSONObject("data")?.optString("cache_status") == "revalidate" ||
			(images.isNotEmpty() && images.all { url ->
				val path = "https://api.lunarx.to/".toHttpUrl().resolve(url)?.encodedPath.orEmpty()
				path.startsWith("/api/cdn/p/")
			})

	/** Sign using the site's existing non-exportable key; polling never creates or replaces keys. */
	fun proofScript(method: String, url: String): String = """
		(function() {
			var method = ${JSONObject.quote(method)}, url = ${JSONObject.quote(url)};
			var key = method + " " + url;
			var state = window.__kaisokuLunarProof;
			if (state && state.key === key) return state.done ? JSON.stringify(state.result) : null;
			state = {key: key, done: false, result: null};
			window.__kaisokuLunarProof = state;
			function openExisting(name) {
				return new Promise(function(resolve) {
					var request = indexedDB.open(name);
					request.onupgradeneeded = function() { request.transaction.abort(); };
					request.onerror = function() { resolve(null); };
					request.onsuccess = function() { resolve(request.result); };
				});
			}
			function read(db, store, name) {
				return new Promise(function(resolve) {
					if (!db || !db.objectStoreNames.contains(store)) return resolve(null);
					var request = db.transaction(store, "readonly").objectStore(store).get(name);
					request.onerror = function() { resolve(null); };
					request.onsuccess = function() { resolve(request.result); };
				});
			}
			function encode(bytes) {
				var text = "";
				for (var i = 0; i < bytes.length; i++) text += String.fromCharCode(bytes[i]);
				return btoa(text).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
			}
			(async function() {
				var db = null, readerDb = null;
				try {
					db = await openExisting("lunar-serenity");
					var pair = await read(db, "keys", "dpop-p256");
					if (!pair || !pair.publicKey || !pair.privateKey) {
						state.result = {validationRequired: true};
						return;
					}
					var exported = await crypto.subtle.exportKey("jwk", pair.publicKey);
					var jwk = {kty: exported.kty, crv: exported.crv, x: exported.x, y: exported.y};
					var encoder = new TextEncoder();
					var parsed = new URL(url);
					var header = {alg: "ES256", typ: "dpop+jwt", jwk: jwk};
					var payload = {
						htu: parsed.origin + parsed.pathname, htm: method.toUpperCase(),
						iat: Math.floor(Date.now() / 1000),
						jti: crypto.randomUUID ? crypto.randomUUID() : Date.now() + "-" + Math.random()
					};
					var message = encode(encoder.encode(JSON.stringify(header))) + "." +
						encode(encoder.encode(JSON.stringify(payload)));
					var signature = await crypto.subtle.sign(
						{name: "ECDSA", hash: "SHA-256"}, pair.privateKey, encoder.encode(message)
					);
					readerDb = await openExisting("keyval-store");
					var metadata = await read(readerDb, "keyval", "workbox-precache-v2");
					var record = metadata && metadata.pub && metadata.pub.length ?
						await read(readerDb, "keyval", metadata.pub[metadata.sel || 0]) :
						await read(readerDb, "keyval", "device-key-secure");
					state.result = {
						proof: message + "." + encode(new Uint8Array(signature)),
						publicJwk: record && (record.w || record.publicJwk) || null
					};
				} catch (error) {
					state.result = {validationRequired: true};
				} finally {
					if (db) db.close();
					if (readerDb) readerDb.close();
					state.done = true;
				}
			})();
			return null;
		})();
	""".trimIndent()
}

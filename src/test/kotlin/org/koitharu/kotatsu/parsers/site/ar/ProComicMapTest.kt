package org.koitharu.kotatsu.parsers.site.ar

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException

internal class ProComicMapTest {
    @Test fun `complete map preserves piece order and coordinates`() {
        val map = parseProComicMap(fixture())
        assertEquals(10, map.width)
        assertEquals(20, map.height)
        assertEquals(listOf("https://cdn.test/left", "https://cdn.test/right"), map.pieces.map { it.url })
        assertEquals(listOf(0, 5), map.pieces.map { it.left })
    }

    @Test fun `oversized dimensions and multiplication overflow are rejected before allocation`() {
        for ((w, h) in listOf(32_768 to 32_768, Int.MAX_VALUE to Int.MAX_VALUE, 0 to 5, -1 to 5)) {
            assertThrows(IOException::class.java) { parseProComicMap(fixture().put("dim", org.json.JSONArray(listOf(w, h)))) }
        }
        assertThrows(IOException::class.java) { parseProComicMap(fixture().put("dim", org.json.JSONArray(listOf(1_000, 1_000))), 16L * 1024 * 1024) }
    }

    @Test fun `missing out of range and unsafe piece addresses fail explicitly`() {
        for (url in listOf("http://cdn.test/left", "file:///tmp/private", "https://procomic.pieces/again")) {
            assertThrows(IOException::class.java) { parseProComicMap(fixture().apply { getJSONArray("pieces").put(0, url) }) }
        }
        assertThrows(IOException::class.java) { parseProComicMap(fixture().apply { getJSONArray("order").put(0, 9) }) }
        assertThrows(IOException::class.java) { parseProComicMap(fixture().apply { getJSONArray("rects").remove(1) }) }
    }

    @Test fun `gaps overlap and integer rectangle overflow cannot produce partial output`() {
        for (change in listOf("left" to -1, "width" to 4, "width" to Int.MAX_VALUE, "top" to 1)) {
            assertThrows(IOException::class.java) { parseProComicMap(fixture().apply {
                getJSONArray("rects").getJSONObject(0).put(change.first, change.second)
            }) }
        }
        assertThrows(IOException::class.java) { parseProComicMap(fixture().apply {
            getJSONArray("rects").getJSONObject(1).put("left", 0)
        }) }
    }

    private fun fixture() = JSONObject("""{"dim":[10,20],"pieces":["https://cdn.test/left","https://cdn.test/right"],"order":[0,1],"rects":[{"left":0,"top":0,"width":5,"height":20},{"left":5,"top":0,"width":5,"height":20}]}""")
}

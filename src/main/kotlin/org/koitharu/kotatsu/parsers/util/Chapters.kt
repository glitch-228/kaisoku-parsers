package org.koitharu.kotatsu.parsers.util

import org.json.JSONArray
import org.json.JSONObject
import org.koitharu.kotatsu.parsers.InternalParsersApi
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.json.asTypedList

private val CHAPTER_NUMBER_REGEX = Regex(
	"""\b(?:chapter|ch\.?|chapitre|cap[ií]tulo|cap\.?|episode|ep\.?)\s*(\d+(?:[.,]\d+)?)""",
	RegexOption.IGNORE_CASE,
)
private val FALLBACK_NUMBER_REGEX = Regex("""(\d+(?:[.,]\d+)?)""")

/** Extracts the labeled chapter number, or the first number when the label is absent. */
public fun String.extractChapterNumber(): Float {
	val value = CHAPTER_NUMBER_REGEX.find(this)?.groupValues?.get(1)
		?: FALLBACK_NUMBER_REGEX.find(this)?.value
	return value?.replace(',', '.')?.toFloatOrNull() ?: 0f
}

@InternalParsersApi
public inline fun <T> List<T>.mapChapters(
	reversed: Boolean = false,
	transform: (index: Int, T) -> MangaChapter?,
): List<MangaChapter> {
	val builder = ChaptersListBuilder(collectionSize())
	var index = 0
	val elements = if (reversed) this.asReversed() else this
	for (item in elements) {
		if (builder.add(transform(index, item))) {
			index++
		}
	}
	return builder.toList()
}

@InternalParsersApi
public inline fun JSONArray.mapChapters(
	reversed: Boolean = false,
	transform: (index: Int, JSONObject) -> MangaChapter?,
): List<MangaChapter> = asTypedList<JSONObject>().mapChapters(reversed, transform)

@InternalParsersApi
public inline fun <T> List<T>.flatMapChapters(
	reversed: Boolean = false,
	transform: (T) -> Iterable<MangaChapter?>,
): List<MangaChapter> {
	val builder = ChaptersListBuilder(collectionSize())
	val elements = if (reversed) this.asReversed() else this
	for (item in elements) {
		builder.addAll(transform(item))
	}
	return builder.toList()
}

@PublishedApi
internal fun <T> Iterable<T>.collectionSize(): Int {
	return if (this is Collection<*>) this.size else 10
}

@PublishedApi
internal class ChaptersListBuilder(initialSize: Int) {

	private val ids = HashSet<Long>(initialSize)
	private val list = ArrayList<MangaChapter>(initialSize)

	fun add(chapter: MangaChapter?): Boolean {
		return chapter != null && ids.add(chapter.id) && list.add(chapter)
	}

	fun addAll(chapters: Iterable<MangaChapter?>) {
		if (chapters is Collection<*>) {
			list.ensureCapacity(list.size + chapters.size)
		}
		chapters.forEach { add(it) }
	}

	operator fun plusAssign(chapter: MangaChapter?) {
		add(chapter)
	}

	fun reverse() {
		list.reverse()
	}

	fun toList(): List<MangaChapter> = list
}

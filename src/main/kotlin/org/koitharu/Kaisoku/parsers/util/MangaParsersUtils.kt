@file:JvmName("MangaParsersUtils")

package org.koitharu.Kaisoku.parsers.util

import org.koitharu.Kaisoku.parsers.model.MangaChapter
import org.koitharu.Kaisoku.parsers.model.MangaListFilter
import kotlin.contracts.contract

public fun MangaListFilter?.isNullOrEmpty(): Boolean {
	contract {
		returns(false) implies (this@isNullOrEmpty != null)
	}
	return this == null || this.isEmpty()
}

public fun Collection<MangaChapter>.findById(chapterId: Long): MangaChapter? = find { x ->
	x.id == chapterId
}

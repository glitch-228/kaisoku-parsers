package org.koitharu.Kaisoku.parsers.exception

import okio.IOException
import org.koitharu.Kaisoku.parsers.InternalParsersApi
import org.koitharu.Kaisoku.parsers.model.MangaSource

/**
 * Authorization is required for access to the requested content
 */
public class AuthRequiredException @InternalParsersApi @JvmOverloads constructor(
	public val source: MangaSource,
	cause: Throwable? = null,
) : IOException("Authorization required", cause)

package org.koitharu.Kaisoku.parsers.config

public interface MangaSourceConfig {

	public operator fun <T> get(key: ConfigKey<T>): T
}

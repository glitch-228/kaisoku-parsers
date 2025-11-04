package org.koitharu.Kaisoku.parsers

import org.koitharu.Kaisoku.parsers.config.ConfigKey
import org.koitharu.Kaisoku.parsers.config.MangaSourceConfig

internal class SourceConfigMock : MangaSourceConfig {

	override fun <T> get(key: ConfigKey<T>): T = key.defaultValue
}
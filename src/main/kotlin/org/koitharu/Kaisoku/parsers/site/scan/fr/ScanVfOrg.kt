package org.koitharu.Kaisoku.parsers.site.scan.fr

import org.koitharu.Kaisoku.parsers.MangaLoaderContext
import org.koitharu.Kaisoku.parsers.MangaSourceParser
import org.koitharu.Kaisoku.parsers.model.MangaParserSource
import org.koitharu.Kaisoku.parsers.site.scan.ScanParser

@MangaSourceParser("SCANVFORG", "ScanVf.org", "fr")
internal class ScanVfOrg(context: MangaLoaderContext) :
	ScanParser(context, MangaParserSource.SCANVFORG, "scanvf.org")

package app.badcat.engine

/** Read-only view of the persisted Legado replacement-rule catalog. */
fun interface LegadoReplaceRuleCatalog {
    suspend fun all(): EngineResult<List<LegadoReplaceRuleRecord>>
}

/**
 * Applies the currently persisted Legado rules to chapter text.
 *
 * The catalog is read for every chapter so rule imports and enabled-state
 * changes take effect without rebuilding the reading engine.
 */
class CatalogLegadoBookTextReplacement(
    private val catalog: LegadoReplaceRuleCatalog,
    private val fallback: BookTextReplacement = BookTextReplacement { it },
) : ScopedBookTextReplacement {
    override suspend fun replace(
        chapterText: String,
        bookName: String,
        bookOrigin: String,
    ): String {
        val records = when (val result = catalog.all()) {
            is EngineResult.Success -> result.value
            is EngineResult.Failure -> throw IllegalStateException(
                "Unable to load Legado replacement rules: ${result.message}",
                result.cause,
            )
        }
        val replaced = LegadoLiteralReplacement(
            rules = records.map { it.executableLiteral() },
            bookName = bookName,
            bookOrigin = bookOrigin,
        ).replace(chapterText)
        return fallback.replace(replaced)
    }
}

package app.badcat.client.runtime

import app.badcat.client.FileReplaceRuleCatalog
import app.badcat.engine.BookTextReplacement
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoLiteralReplacement
import app.badcat.engine.ScopedBookTextReplacement

/**
 * Loads the latest persisted rule snapshot for each chapter and applies the
 * supported literal subset with Legado content scope selection.
 *
 * Regex rules deliberately fail closed in LegadoLiteralReplacement until a
 * bounded, semantically compatible regex runtime is available.
 */
class CatalogLegadoBookTextReplacement(
    private val catalog: FileReplaceRuleCatalog,
    private val fallback: BookTextReplacement,
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

package app.badcat.engine

/** Read-only view of the persisted Legado replacement-rule catalog. */
interface LegadoReplaceRuleCatalog {
    suspend fun all(): EngineResult<List<LegadoReplaceRuleRecord>>

    /** Persistence adapters should disable a rule after a regex timeout. */
    suspend fun setEnabled(ruleId: Long, enabled: Boolean): EngineResult<Unit> =
        EngineResult.Success(Unit)
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
    private val jsEvaluator: LegadoJsReplacementEvaluator? = null,
) : ScopedBookTextReplacement {
    override suspend fun replace(
        chapterTitle: String,
        chapterText: String,
        context: LegadoReplacementContext,
    ): ReplacedChapterContent {
        val records = when (val result = catalog.all()) {
            is EngineResult.Success -> result.value
            is EngineResult.Failure -> throw IllegalStateException(
                "Unable to load Legado replacement rules: ${result.message}",
                result.cause,
            )
        }
        val executableRules = records.map { it.executableLiteral() }
        val replacedTitle = applyRules(
            executableRules,
            chapterTitle,
            context,
            LegadoReplaceRuleTarget.TITLE,
        )
        val replacedText = applyRules(
            executableRules,
            chapterText,
            context,
            LegadoReplaceRuleTarget.CONTENT,
        )
        return ReplacedChapterContent(replacedTitle, fallback.replace(replacedText))
    }

    private suspend fun applyRules(
        rules: List<LegadoReplaceRule>,
        input: String,
        context: LegadoReplacementContext,
        target: LegadoReplaceRuleTarget,
    ): String {
        return try {
            LegadoLiteralReplacement(
                rules = rules,
                bookName = context.book.name,
                bookOrigin = context.book.origin,
                target = target,
                replacementContext = context,
                jsEvaluator = jsEvaluator,
            ).replace(input)
        } catch (timeout: LegadoRegexTimeoutException) {
            val disabled = runCatching { catalog.setEnabled(timeout.ruleId, false) }
                .getOrElse {
                    timeout.addSuppressed(it)
                    null
                }
            if (disabled is EngineResult.Failure) {
                timeout.addSuppressed(
                    IllegalStateException("Unable to disable timed-out rule '${timeout.ruleId}': ${disabled.message}", disabled.cause)
                )
            }
            throw timeout
        }
    }
}

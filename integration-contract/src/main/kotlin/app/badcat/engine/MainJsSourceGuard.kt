package app.badcat.engine

/**
 * Prevents a pure mainJs source from being silently interpreted as declarative
 * CSS rules. The dedicated JavaScript source pipeline is intentionally separate.
 */
object MainJsSourceGuard {
    fun failure(source: BookSource, stage: String): EngineResult.Failure? {
        if (source.mainJs.isNullOrBlank()) return null
        return EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Book source '${source.id}' uses mainJs; the dedicated JavaScript ${stage} pipeline is not available yet",
        )
    }
}

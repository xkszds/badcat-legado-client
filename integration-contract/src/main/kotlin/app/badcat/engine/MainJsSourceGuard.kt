package app.badcat.engine

/**
 * Prevents a mainJs source from being interpreted as declarative rules when
 * no script runtime is available. Services with a bounded runtime may execute
 * mainJs before their per-field scripts.
 */
object MainJsSourceGuard {
    fun failure(
        source: BookSource,
        stage: String,
        scriptRuntimeAvailable: Boolean = false,
    ): EngineResult.Failure? {
        if (source.mainJs.isNullOrBlank() || scriptRuntimeAvailable) return null
        return EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Book source '${source.id}' uses mainJs, but no bounded JavaScript runtime is configured for $stage",
        )
    }
}

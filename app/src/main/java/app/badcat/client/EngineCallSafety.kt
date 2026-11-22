package app.badcat.client

import app.badcat.engine.EngineErrorCode
import app.badcat.engine.EngineResult
import kotlinx.coroutines.CancellationException

/** Converts unexpected adapter exceptions to typed failures, preserving cancellation. */
internal suspend fun <T> safeEngineCall(
    operation: suspend () -> EngineResult<T>,
): EngineResult<T> = try {
    operation()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    EngineResult.Failure(
        EngineErrorCode.UNKNOWN,
        error.message ?: "阅读引擎发生未预期异常",
        error,
    )
}

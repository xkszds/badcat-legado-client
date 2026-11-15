package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LegadoEmbeddedUrlTemplateTest {

    @Test
    fun evaluatesMultipleEmbeddedExpressionsInOrder() = kotlinx.coroutines.runBlocking {
        val result = LegadoEmbeddedUrlTemplate.resolve(
            "https://example.test/search?q={{key}}&page={{page}}",
            "雪猫",
            LegadoEmbeddedScriptEvaluator { script, key, page ->
                EngineResult.Success(if (script == "key") key else page)
            },
            page = 3,
        )
        assertEquals(
            "https://example.test/search?q=雪猫&page=3",
            assertIs<EngineResult.Success<String>>(result).value,
        )
    }

    @Test
    fun convertsNullAndIntegralDoubleLikePinnedUpstream() = kotlinx.coroutines.runBlocking {
        val nullResult = LegadoEmbeddedUrlTemplate.resolve(
            "a{{empty}}b",
            "x",
            LegadoEmbeddedScriptEvaluator { _, _, _ -> EngineResult.Success(null) },
        )
        val numberResult = LegadoEmbeddedUrlTemplate.resolve(
            "page={{page}}",
            "x",
            LegadoEmbeddedScriptEvaluator { _, _, _ -> EngineResult.Success(12.0) },
        )
        assertEquals("ab", assertIs<EngineResult.Success<String>>(nullResult).value)
        assertEquals("page=12", assertIs<EngineResult.Success<String>>(numberResult).value)
    }

    @Test
    fun leavesUnclosedMarkerLiteral() = kotlinx.coroutines.runBlocking {
        val result = LegadoEmbeddedUrlTemplate.resolve(
            "https://example.test/{{key",
            "x",
            LegadoEmbeddedScriptEvaluator { _, _, _ -> error("must not evaluate") },
        )
        assertEquals(
            "https://example.test/{{key",
            assertIs<EngineResult.Success<String>>(result).value,
        )
    }

    @Test
    fun propagatesScriptFailureWithoutSendingPartialUrl() = kotlinx.coroutines.runBlocking {
        val result = LegadoEmbeddedUrlTemplate.resolve(
            "https://example.test/{{key}}",
            "x",
            LegadoEmbeddedScriptEvaluator { _, _, _ ->
                EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "disabled")
            },
        )
        assertEquals(
            EngineErrorCode.UNSUPPORTED_RULE,
            assertIs<EngineResult.Failure>(result).code,
        )
    }
}

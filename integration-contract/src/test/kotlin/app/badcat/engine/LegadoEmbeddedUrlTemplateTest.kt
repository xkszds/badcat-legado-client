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
    @Test
    fun expandsPersistentGetAndPutTokensBeforeEmbeddedJavaScript() = kotlinx.coroutines.runBlocking {
        val values = mutableMapOf<String, String>()
        val source = BookSource(
            id = "https://variables.example/",
            name = "Variables",
            baseUrl = "https://variables.example/",
        )
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> {
                assertEquals("render(result)", script)
                assertEquals("page body", bindings["result"])
                return EngineResult.Success("rendered")
            }

            override suspend fun getString(source: BookSource, key: String): EngineResult<String?> =
                EngineResult.Success(values[key])

            override suspend fun putString(source: BookSource, key: String, value: String): EngineResult<Unit> {
                values[key] = value
                return EngineResult.Success(Unit)
            }
        }

        val result = LegadoEmbeddedRuleTemplate.resolve(
            source = source,
            template = """@put:{"token":"saved"}https://variables.example/{{render(result)}}?v=@get:{token}""",
            result = "page body",
            baseUrl = "https://variables.example/",
            ruleName = "bookList",
            scriptRuntime = runtime,
        )

        assertEquals("https://variables.example/rendered?v=saved", assertIs<EngineResult.Success<String>>(result).value)
        assertEquals("saved", values["token"])
    }

}

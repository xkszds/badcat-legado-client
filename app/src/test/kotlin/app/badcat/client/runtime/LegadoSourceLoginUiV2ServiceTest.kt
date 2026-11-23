package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineErrorCode
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoLoginUiV2
import app.badcat.engine.SourceScriptRuntime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegadoSourceLoginUiV2ServiceTest {
    private class RecordingRuntime(
        private val outputs: ArrayDeque<String>,
    ) : SourceScriptRuntime {
        var capturedScript: String? = null
        var capturedSource: BookSource? = null
        var capturedBindings: Map<String, String> = emptyMap()

        override suspend fun evaluate(
            source: BookSource,
            script: String,
            bindings: Map<String, String>,
        ): EngineResult<String> {
            capturedSource = source
            capturedScript = script
            capturedBindings = bindings
            return EngineResult.Success(outputs.removeFirst())
        }
    }

    private fun source() = BookSource(
        id = "https://login.example/",
        name = "Login source",
        baseUrl = "https://login.example/",
        loginUrl = "<js>function loginUi(state){} function loginAction(action,state,form){}</js>",
        loginUi = """{"version":2}""",
        loginCheckJs = "true",
    )

    @Test
    fun rendersLoginRowsThroughSourceScopedScriptRuntime() = runBlocking {
        val runtime = RecordingRuntime(ArrayDeque(listOf("""{"rows":[{"name":"账号","type":"text","key":"username"}]}""")))
        val service = LegadoSourceLoginUiV2Service(runtime)
        val result = service.render(source(), """{"step":1}""")
        val rows = (result as EngineResult.Success<List<LegadoLoginUiV2.Row>>).value
        assertEquals("username", rows.single().key)
        assertTrue(requireNotNull(runtime.capturedScript).contains("loginUi(JSON.parse(String(__loginState)))"))
        assertEquals("""{"step":1}""", runtime.capturedBindings["__loginState"])
    }

    @Test
    fun dispatchesActionWithJsonStateAndFormAndParsesResult() = runBlocking {
        val runtime = RecordingRuntime(ArrayDeque(listOf("""{"state":{"step":2},"error":{"code":"验证码错误"},"close":false}""")))
        val service = LegadoSourceLoginUiV2Service(runtime)
        val result = service.dispatchAction(
            source(),
            "sendCode",
            """{"step":1}""",
            """{"phone":"123"}""",
        )
        val action = (result as EngineResult.Success<LegadoLoginUiV2.ActionResult>).value
        assertEquals("""{"step":2}""", action.stateJson)
        assertEquals("验证码错误", action.errors["code"])
        assertTrue(requireNotNull(runtime.capturedScript).contains("loginAction(String(__loginAction)"))
        assertEquals("sendCode", runtime.capturedBindings["__loginAction"])
        assertEquals("""{"phone":"123"}""", runtime.capturedBindings["__loginForm"])
    }

    @Test
    fun preservesSameTypedInvocationContextAcrossRenderAndActionDispatch() = runBlocking {
        val invocation = LegadoLoginUiV2.InvocationContext(
            sourceId = "https://login.example/",
            bookType = 8 or 32,
            bookUrl = "https://book.example/book",
            chapterUrl = "https://book.example/chapter/1",
        )
        val renderRuntime = RecordingRuntime(ArrayDeque(listOf("""{"rows":[{"name":"继续","type":"button","action":"next"}]}""")))
        val service = LegadoSourceLoginUiV2Service(renderRuntime)
        assertTrue(service.render(source(), invocation = invocation) is EngineResult.Success)
        val expected = invocation.toJson()
        assertEquals(expected, renderRuntime.capturedBindings["__badcatLoginInvocation"])

        val actionRuntime = RecordingRuntime(ArrayDeque(listOf("""{"state":{},"close":false}""")))
        val actionResult = LegadoSourceLoginUiV2Service(actionRuntime)
            .dispatchAction(source(), "next", "{}", "{}", invocation)
        assertTrue(actionResult is EngineResult.Success)
        assertEquals(expected, actionRuntime.capturedBindings["__badcatLoginInvocation"])
    }

    @Test
    fun rejectsMismatchedOrInvalidInvocationContextBeforeRunningScript() = runBlocking {
        val runtime = RecordingRuntime(ArrayDeque(emptyList()))
        val service = LegadoSourceLoginUiV2Service(runtime)
        val invalid = service.render(
            source(),
            invocation = LegadoLoginUiV2.InvocationContext(source().id, bookType = 1),
        )
        assertEquals(EngineErrorCode.INVALID_SOURCE, (invalid as EngineResult.Failure).code)
        val mismatched = service.dispatchAction(
            source(), "submit", "{}", "{}",
            LegadoLoginUiV2.InvocationContext("https://other.example/"),
        )
        assertEquals(EngineErrorCode.INVALID_SOURCE, (mismatched as EngineResult.Failure).code)
        assertEquals(null, runtime.capturedScript)
    }

    @Test
    fun validatesLoginCheckResultAndRejectsNonBooleanValues() = runBlocking {
        val trueRuntime = RecordingRuntime(ArrayDeque(listOf("true")))
        val invalidRuntime = RecordingRuntime(ArrayDeque(listOf("not-a-boolean")))
        assertEquals(
            true,
            (LegadoSourceLoginUiV2Service(trueRuntime).checkLogin(source()) as EngineResult.Success).value,
        )
        val failure = LegadoSourceLoginUiV2Service(invalidRuntime).checkLogin(source()) as EngineResult.Failure
        assertEquals(EngineErrorCode.PARSE, failure.code)
    }

    @Test
    fun rendersLoginUiV2ForSourcesWithMainJsInitialization() = runBlocking {
        val runtime = RecordingRuntime(ArrayDeque(listOf("""{"rows":[{"name":"账号","type":"text","key":"username"}]}""")))
        val service = LegadoSourceLoginUiV2Service(runtime)
        val mainJs = "function sharedLoginHelper(){ return true; }\nfunction loginUi(state){ return {rows:[]}; }"
        val mainJsSource = source().copy(
            mainJs = mainJs,
            loginUrl = "<js>function legacyLoginUi(){ return 'must-not-run'; }</js>",
        )
        val result = service.render(mainJsSource)
        assertTrue(result is EngineResult.Success)
        assertEquals(mainJsSource.mainJs, runtime.capturedSource?.mainJs)
        val executed = requireNotNull(runtime.capturedScript)
        assertTrue(executed.startsWith(mainJs))
        assertTrue(executed.contains("loginUi(JSON.parse(String(__loginState)))"))
        assertTrue(!executed.contains("legacyLoginUi"))
    }

    @Test
    fun dispatchesLoginActionsForSourcesWithMainJsInitialization() = runBlocking {
        val runtime = RecordingRuntime(ArrayDeque(listOf("""{"state":{"step":2},"close":true}""")))
        val service = LegadoSourceLoginUiV2Service(runtime)
        val mainJs = "function sharedLoginHelper(){ return true; }\nfunction loginAction(action,state,form){ return {state:state,close:true}; }"
        val mainJsSource = source().copy(
            mainJs = mainJs,
            loginUrl = "<js>function legacyLoginAction(){ return 'must-not-run'; }</js>",
        )
        val result = service.dispatchAction(mainJsSource, "submit", "{}", "{}")
        assertTrue(result is EngineResult.Success)
        assertEquals(mainJsSource.mainJs, runtime.capturedSource?.mainJs)
        val executed = requireNotNull(runtime.capturedScript)
        assertTrue(executed.startsWith(mainJs))
        assertTrue(executed.contains("loginAction(String(__loginAction)"))
        assertTrue(!executed.contains("legacyLoginAction"))
    }

    @Test
    fun fallsBackToLoginUrlWhenMainJsIsBlankAndSupportsAtJsPrefix() = runBlocking {
        val runtime = RecordingRuntime(ArrayDeque(listOf("""{"rows":[{"name":"账号","type":"text","key":"username"}]}""")))
        val service = LegadoSourceLoginUiV2Service(runtime)
        val atJsSource = source().copy(
            mainJs = "  ",
            loginUrl = "@js: function loginUi(state){ return {rows:[]}; }",
        )
        val result = service.render(atJsSource)
        assertTrue(result is EngineResult.Success)
        val executed = requireNotNull(runtime.capturedScript)
        assertTrue(executed.startsWith("function loginUi(state)"))
        assertTrue(executed.contains("loginUi(JSON.parse(String(__loginState)))"))
    }

    @Test
    fun rejectsLegacyFormsAndMalformedStateBeforeScriptExecution() = runBlocking {
        val runtime = RecordingRuntime(ArrayDeque(listOf("{}")))
        val service = LegadoSourceLoginUiV2Service(runtime)
        val legacy = source().copy(loginUi = """[{"name":"账号","type":"text"}]""")
        val legacyResult = service.render(legacy)
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, (legacyResult as EngineResult.Failure).code)
        val invalidState = service.render(source(), "[]")
        assertEquals(EngineErrorCode.INVALID_SOURCE, (invalidState as EngineResult.Failure).code)
        assertEquals(null, runtime.capturedScript)
    }
}

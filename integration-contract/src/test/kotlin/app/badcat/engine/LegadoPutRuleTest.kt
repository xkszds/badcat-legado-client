package app.badcat.engine

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LegadoPutRuleTest {
    @Test
    fun parsesUnquotedAssignmentsAndKeepsNestedSelectorPunctuation() {
        val parsed = assertIs<EngineResult.Success<LegadoPutRule.Parsed>>(
            LegadoPutRule.parse("""@put:{name:"meta[property='book,name']@content",author:'span.author@text'} @get:{name}"""),
        ).value

        assertEquals(2, parsed.assignments.size)
        assertEquals("name", parsed.assignments[0].key)
        assertEquals("meta[property='book,name']@content", parsed.assignments[0].rule)
        assertEquals("author", parsed.assignments[1].key)
        assertEquals("span.author@text", parsed.assignments[1].rule)
        assertEquals("@get:{name}", parsed.expression)
    }

    @Test
    fun evaluatesJavaScriptPutAssignmentsAndExpandsGetTokens() = runBlocking {
        val source = BookSource(
            id = "https://put-js.example/",
            name = "Put JS",
            baseUrl = "https://put-js.example/",
        )
        val values = mutableMapOf<String, String>()
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = EngineResult.Success(
                when (script) {
                    "result + ':' + page" -> bindings.getValue("result") + ":" + bindings.getValue("page")
                    else -> script
                },
            )
            override suspend fun getString(source: BookSource, key: String): EngineResult<String?> =
                EngineResult.Success(values[key])
            override suspend fun putString(source: BookSource, key: String, value: String): EngineResult<Unit> {
                values[key] = value
                return EngineResult.Success(Unit)
            }
        }
        val document = SourceDocument(
            url = "https://put-js.example/book/1",
            body = "<h1>Novel</h1>",
            page = 3,
        )

        val result = LegadoPutRule.resolve(
            source,
            "@put:{payload:@js:result + ':' + page} @get:{payload}",
            document,
            runtime,
        )

        assertEquals(" <h1>Novel</h1>:3", (result as EngineResult.Success).value)
        assertEquals("<h1>Novel</h1>:3", values["payload"])
    }
}

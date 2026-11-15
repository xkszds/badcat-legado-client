package app.badcat.engine

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BookSourceSearchUrlResolverTest {
    private val resolver = BookSourceSearchUrlResolver()
    private fun source(searchUrl: String? = "/search?q={{key}}", enabled: Boolean = true) =
        BookSource(
            id = "https://reader.example",
            name = "Fixture",
            baseUrl = "https://reader.example",
            enabled = enabled,
            searchUrl = searchUrl,
        )

    @Test
    fun returnsRawTemplateForGatewayExpansion() = runBlocking {
        val result = resolver.resolve(source(), "雪猫")
        assertEquals(
            "/search?q={{key}}",
            assertIs<EngineResult.Success<String>>(result).value,
        )
    }

    @Test
    fun rejectsMissingSearchUrl() = runBlocking {
        val result = resolver.resolve(source(searchUrl = null), "cat")
        assertEquals(
            EngineErrorCode.INVALID_SOURCE,
            assertIs<EngineResult.Failure>(result).code,
        )
    }

    @Test
    fun rejectsUnsupportedAtJsForm() = runBlocking {
        val result = resolver.resolve(source("/search@js"), "cat")
        assertEquals(
            EngineErrorCode.UNSUPPORTED_RULE,
            assertIs<EngineResult.Failure>(result).code,
        )
    }

    @Test
    fun rejectsDisabledSource() = runBlocking {
        val result = resolver.resolve(source(enabled = false), "cat")
        assertEquals(
            EngineErrorCode.INVALID_SOURCE,
            assertIs<EngineResult.Failure>(result).code,
        )
    }
}

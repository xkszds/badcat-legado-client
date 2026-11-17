package app.badcat.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainJsSourceGuardTest {
    @Test
    fun mainJsRequiresRuntimeOnlyWhenRuntimeIsAbsent() {
        val source = BookSource(
            id = "main-js-source",
            name = "mainJs source",
            baseUrl = "https://example.invalid/",
            mainJs = "function helper() { return 'ok'; }",
        )

        val blocked = MainJsSourceGuard.failure(source, "search")
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, blocked?.code)

        assertNull(
            MainJsSourceGuard.failure(
                source,
                "search",
                scriptRuntimeAvailable = true,
            ),
        )
    }

    @Test
    fun sourceWithoutMainJsDoesNotRequireRuntime() {
        val source = BookSource(
            id = "plain-source",
            name = "plain source",
            baseUrl = "https://example.invalid/",
        )

        assertNull(MainJsSourceGuard.failure(source, "search"))
    }
}

package app.badcat.client.runtime

import app.badcat.engine.LegadoJsReplacementContext
import app.badcat.engine.LegadoRegexTimeoutException
import app.badcat.engine.LegadoReplacementBookContext
import app.badcat.engine.LegadoReplacementChapterContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RhinoLegadoJsReplacementEvaluatorTest {
    private fun context(
        result: String = "123",
        timeout: Long = 1_000,
    ) = LegadoJsReplacementContext(
        ruleId = 17,
        ruleName = "fixture @js",
        result = result,
        timeoutMillisecond = timeout,
        configuredTimeoutMillisecond = timeout,
        book = LegadoReplacementBookContext(
            bookUrl = "https://source.example/book/1",
            origin = "https://source.example/",
            originName = "Fixture Source",
            name = "Fixture Book",
        ),
        chapter = LegadoReplacementChapterContext(
            url = "https://source.example/chapter/1",
            title = "Chapter 1",
            bookUrl = "https://source.example/book/1",
            index = 0,
            baseUrl = "https://source.example/",
        ),
    )

    @Test
    fun evaluatesResultBookAndChapterBindings() {
        val session = RhinoLegadoJsReplacementEvaluator().createSession("fixture @js")

        val result = session.evaluate(
            "book.name + ':' + chapter.title + ':' + result.toUpperCase()",
            context(),
        )

        assertEquals("Fixture Book:Chapter 1:123", result)
    }

    @Test
    fun keepsJavaHelperVariablesAcrossMatchesAndExposesDigestHelpers() {
        val session = RhinoLegadoJsReplacementEvaluator().createSession("fixture @js")

        session.evaluate("java.put('memo', result)", context(result = "stored"))
        val remembered = session.evaluate("java.get('memo')", context(result = "next"))
        val md5 = session.evaluate("java.md5Encode('abc')", context())

        assertEquals("stored", remembered)
        assertEquals("900150983cd24fb0d6963f7d28e17f72", md5)
    }

    @Test
    fun interruptsInfiniteScriptsWithinRuleTimeout() {
        val session = RhinoLegadoJsReplacementEvaluator().createSession("timeout fixture")

        assertThrows(LegadoRegexTimeoutException::class.java) {
            session.evaluate("while (true) {}", context(timeout = 250))
        }
    }
    @Test
    fun rejectsOversizedScriptBeforeEvaluation() {
        val session = RhinoLegadoJsReplacementEvaluator().createSession("oversized fixture")

        assertThrows(IllegalArgumentException::class.java) {
            session.evaluate(" ".repeat(128 * 1024 + 1), context())
        }
    }

    @Test
    fun rejectsOversizedContextBeforeBindingCreation() {
        val session = RhinoLegadoJsReplacementEvaluator().createSession("oversized context fixture")

        assertThrows(IllegalArgumentException::class.java) {
            session.evaluate("result", context(result = "x".repeat(64 * 1024 + 1)))
        }
    }


    @Test
    fun blocksJavaRuntimeClassAccess() {
        val session = RhinoLegadoJsReplacementEvaluator().createSession("class shutter fixture")

        assertThrows(UnsupportedOperationException::class.java) {
            session.evaluate("Packages.java.lang.Runtime.getRuntime()", context())
        }
    }

    @Test
    fun doesNotShareHelperStateAcrossRuleSessions() {
        val first = RhinoLegadoJsReplacementEvaluator().createSession("first rule")
        val second = RhinoLegadoJsReplacementEvaluator().createSession("second rule")

        first.evaluate("java.put('privateKey', 'first-only')", context())
        assertEquals("", second.evaluate("java.get('privateKey')", context()))
    }

    @Test
    fun blocksProtectedClassesThroughPackagesNamespace() {
        val session = RhinoLegadoJsReplacementEvaluator().createSession("packages fixture")

        assertThrows(UnsupportedOperationException::class.java) {
            session.evaluate("Packages.java.lang.ProcessBuilder", context())
        }
    }

    @Test
    fun blocksReflectiveClassLookupFromExposedHelper() {
        val session = RhinoLegadoJsReplacementEvaluator().createSession("reflection fixture")

        assertThrows(UnsupportedOperationException::class.java) {
            session.evaluate("java.getClass().forName('java.lang.Runtime')", context())
        }
    }

}

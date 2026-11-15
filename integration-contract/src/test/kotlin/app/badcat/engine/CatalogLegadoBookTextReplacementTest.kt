package app.badcat.engine

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogLegadoBookTextReplacementTest {
    private fun context(bookName: String, origin: String) = LegadoReplacementContext(
        book = LegadoReplacementBookContext(
            bookUrl = "https://source.example/book/1",
            origin = origin,
            originName = "Fixture Source",
            name = bookName,
        ),
        chapter = LegadoReplacementChapterContext(
            url = "https://source.example/chapter/1",
            title = "Chapter",
            bookUrl = "https://source.example/book/1",
            index = 0,
            baseUrl = origin,
        ),
    )

    @Test
    fun reloadsPersistedRulesAndAppliesTitleAndContentScopes() = runBlocking {
        val imported = LegadoReplaceRuleCodec.decode(
            """[
                {"id":1,"pattern":"foo","replacement":"bar","isRegex":false,"scopeContent":true,"order":0},
                {"id":2,"pattern":"bar","replacement":"done","isRegex":false,"scopeContent":true,"order":1},
                {"id":3,"pattern":"foo","replacement":"wrong","isRegex":false,"scope":"Another Book","scopeContent":true,"order":0},
                {"id":4,"pattern":"Chapter","replacement":"Part","isRegex":false,"scopeTitle":true,"scopeContent":false,"order":0}
            ]"""
        )
        var catalogReads = 0
        val catalog = object : LegadoReplaceRuleCatalog {
            override suspend fun all(): EngineResult<List<LegadoReplaceRuleRecord>> {
                catalogReads++
                return EngineResult.Success(imported)
            }
        }
        val replacement = CatalogLegadoBookTextReplacement(catalog)

        val first = replacement.replace("Chapter 1", "foo foo", context("Current Book", "https://source.example/"))
        assertEquals("Part 1", first.title)
        assertEquals("done done", first.text)
        assertEquals(1, catalogReads)

        val second = replacement.replace("Chapter 2", "foo", context("Current Book", "https://source.example/"))
        assertEquals("Part 2", second.title)
        assertEquals("done", second.text)
        assertEquals(2, catalogReads)
    }

    @Test
    fun surfacesCatalogReadFailuresInsteadOfSilentlySkippingPurification(): Unit = runBlocking {
        val catalog = object : LegadoReplaceRuleCatalog {
            override suspend fun all(): EngineResult<List<LegadoReplaceRuleRecord>> =
                EngineResult.Failure(EngineErrorCode.UNKNOWN, "catalog unavailable")
        }
        val replacement = CatalogLegadoBookTextReplacement(catalog)

        assertFailsWith<IllegalStateException> {
            replacement.replace("Title", "text", context("Book", "https://source.example/"))
        }
    }

    @Test
    fun disablesRuleWhenRegexExecutionTimesOut() = runBlocking {
        val imported = LegadoReplaceRuleCodec.decode(
            """[{"id":77,"pattern":"^(a+)+$","replacement":"x","isRegex":true,"timeoutMillisecond":1}]"""
        )
        var disabledRuleId: Long? = null
        val catalog = object : LegadoReplaceRuleCatalog {
            override suspend fun all(): EngineResult<List<LegadoReplaceRuleRecord>> =
                EngineResult.Success(imported)

            override suspend fun setEnabled(ruleId: Long, enabled: Boolean): EngineResult<Unit> {
                if (!enabled) disabledRuleId = ruleId
                return EngineResult.Success(Unit)
            }
        }
        val replacement = CatalogLegadoBookTextReplacement(catalog)

        assertFailsWith<LegadoRegexTimeoutException> {
            replacement.replace("Chapter", "a".repeat(4096) + "!", context("Book", "https://source.example/"))
        }
        assertEquals(77L, disabledRuleId)
    }

    @Test
    fun evaluatesJsReplacementWithBookAndChapterContext() = runBlocking {
        val imported = LegadoReplaceRuleCodec.decode(
            """[{"id":88,"name":"Uppercase digits","pattern":"[a-z]+","replacement":"@js:book.name + ':' + chapter.title + ':' + result.toUpperCase()","isRegex":true}]"""
        )
        val catalog = object : LegadoReplaceRuleCatalog {
            override suspend fun all(): EngineResult<List<LegadoReplaceRuleRecord>> =
                EngineResult.Success(imported)
        }
        val evaluator = LegadoJsReplacementEvaluator { script, jsContext ->
            assertEquals("Uppercase digits", jsContext.ruleName)
            assertEquals("Current Book", jsContext.book.name)
            assertEquals("Chapter", jsContext.chapter.title)
            assertEquals("foo", jsContext.result)
            assertEquals("book.name + ':' + chapter.title + ':' + result.toUpperCase()", script)
            jsContext.book.name + ":" + jsContext.chapter.title + ":" + jsContext.result.uppercase()
        }
        val replacement = CatalogLegadoBookTextReplacement(catalog, jsEvaluator = evaluator)

        val result = replacement.replace("Chapter", "foo", context("Current Book", "https://source.example/"))
        assertEquals("Chapter", result.title)
        assertEquals("Current Book:Chapter:FOO", result.text)
    }
}

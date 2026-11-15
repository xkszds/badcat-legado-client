package app.badcat.engine

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogLegadoBookTextReplacementTest {
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

        val first = replacement.replace("Chapter 1", "foo foo", "Current Book", "https://source.example/")
        assertEquals("Part 1", first.title)
        assertEquals("done done", first.text)
        assertEquals(1, catalogReads)

        val second = replacement.replace("Chapter 2", "foo", "Current Book", "https://source.example/")
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
            replacement.replace("Title", "text", "Book", "https://source.example/")
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
            replacement.replace("Chapter", "a".repeat(4096) + "!", "Book", "https://source.example/")
        }
        assertEquals(77L, disabledRuleId)
    }
}

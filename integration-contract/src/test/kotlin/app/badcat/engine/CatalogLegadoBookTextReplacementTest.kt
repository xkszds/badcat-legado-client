package app.badcat.engine

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogLegadoBookTextReplacementTest {
    @Test
    fun reloadsPersistedRulesAndAppliesScopedLiteralReplacement() = runBlocking {
        val imported = LegadoReplaceRuleCodec.decode(
            """[
                {"id":1,"pattern":"foo","replacement":"bar","isRegex":false,"scopeContent":true,"order":0},
                {"id":2,"pattern":"bar","replacement":"done","isRegex":false,"scopeContent":true,"order":1},
                {"id":3,"pattern":"foo","replacement":"wrong","isRegex":false,"scope":"Another Book","scopeContent":true,"order":0}
            ]"""
        )
        var catalogReads = 0
        val catalog = LegadoReplaceRuleCatalog {
            catalogReads++
            EngineResult.Success(imported)
        }
        val replacement = CatalogLegadoBookTextReplacement(catalog)

        assertEquals("done done", replacement.replace("foo foo", "Current Book", "https://source.example/"))
        assertEquals(1, catalogReads)
        assertEquals("done", replacement.replace("foo", "Current Book", "https://source.example/"))
        assertEquals(2, catalogReads)
    }

    @Test
    fun surfacesCatalogReadFailuresInsteadOfSilentlySkippingPurification() = runBlocking {
        val catalog = LegadoReplaceRuleCatalog {
            EngineResult.Failure(EngineErrorCode.UNKNOWN, "catalog unavailable")
        }
        val replacement = CatalogLegadoBookTextReplacement(catalog)

        assertFailsWith<IllegalStateException> {
            replacement.replace("text", "Book", "https://source.example/")
        }
    }
}

package app.badcat.client

import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BadcatAdMarkCatalogTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val catalogFile get() = File(context.filesDir, "badcat-ad-mark-catalog.json")
    private val backupFile get() = File(context.filesDir, "badcat-ad-mark-catalog.json.bak")

    private val first = BadcatAdMarkItem(
        host = "reader.example",
        urlPattern = "[1,2,3]",
        markRules = listOf(".ad-card"),
    )
    private val second = BadcatAdMarkItem(
        host = "other.example",
        urlPattern = "[4,5]",
        markRules = listOf("#sponsor"),
    )

    @Before
    fun clearCatalogFiles() {
        catalogFile.delete()
        backupFile.delete()
    }

    @After
    fun removeCatalogFiles() {
        catalogFile.delete()
        backupFile.delete()
    }

    @Test
    fun catalogPersistsRecordsAcrossCatalogRecreation() {
        BadcatAdMarkCatalog(context).upsert(first)

        val reopened = BadcatAdMarkCatalog(context)
        assertEquals(listOf(first), reopened.all())
        assertEquals(listOf(first), BadcatAdMarkJsonCodec.decode(reopened.exportJson()))
    }

    @Test
    fun upsertReplacesOnlyTheMatchingHostAndUrlPatternIdentity() {
        val catalog = BadcatAdMarkCatalog(context)
        catalog.upsert(first)
        catalog.upsert(second)
        val replacement = first.copy(host = "READER.EXAMPLE", markRules = listOf(".new-ad"))

        catalog.upsert(replacement)

        assertEquals(2, catalog.all().size)
        assertEquals(replacement, catalog.all().single { it.urlPattern == first.urlPattern })
        assertEquals(second, catalog.all().single { it.host == second.host })
    }

    @Test
    fun importMergesRecordsAndReplacesMatchingIdentity() {
        val catalog = BadcatAdMarkCatalog(context)
        catalog.upsert(first)
        catalog.upsert(second)
        val incoming = first.copy(markRules = listOf(".updated"))
        val imported = BadcatAdMarkJsonCodec.encode(listOf(incoming))

        assertEquals(1, catalog.importJson(imported))

        assertEquals(listOf(incoming, second), catalog.all())
    }

    @Test
    fun removeDeletesOnlyTheRequestedIdentity() {
        val catalog = BadcatAdMarkCatalog(context)
        catalog.upsert(first)
        catalog.upsert(second)

        assertTrue(catalog.remove(first.host, first.urlPattern))
        assertFalse(catalog.remove(first.host, first.urlPattern))
        assertEquals(listOf(second), catalog.all())
    }

    @Test
    fun rejectedImportAndReplaceAllLeavePreviouslyPersistedCatalogUntouched() {
        val catalog = BadcatAdMarkCatalog(context)
        catalog.upsert(first)
        catalog.upsert(second)
        val before = catalog.exportJson()
        val malformed = """{"markItems":[{"host":"reader.example","ruleItems":[{"urlPattern":[1,2],"markRules":[1]}]}]}"""
        val duplicate = BadcatAdMarkJsonCodec.encode(listOf(first, first.copy(markRules = listOf(".duplicate"))))

        assertThrows(IllegalArgumentException::class.java) {
            catalog.importJson(malformed)
        }
        assertThrows(IllegalArgumentException::class.java) {
            catalog.importJson(duplicate)
        }
        assertThrows(IllegalArgumentException::class.java) {
            catalog.replaceAll(listOf(first, first.copy(markRules = listOf(".duplicate"))))
        }

        assertEquals(before, catalog.exportJson())
        assertEquals(listOf(first, second), catalog.all())
    }

    @Test
    fun oversizedImportLeavesPreviouslyPersistedCatalogUntouched() {
        val catalog = BadcatAdMarkCatalog(context)
        catalog.upsert(first)
        val before = catalog.exportJson()
        val oversized = " ".repeat(BadcatAdMarkJsonCodec.MAX_JSON_BYTES + 1)

        assertThrows(IllegalArgumentException::class.java) {
            catalog.importJson(oversized)
        }

        assertEquals(before, catalog.exportJson())
    }
}

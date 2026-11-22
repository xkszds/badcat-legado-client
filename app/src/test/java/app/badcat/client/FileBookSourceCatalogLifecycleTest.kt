package app.badcat.client

import app.badcat.engine.EngineResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileBookSourceCatalogLifecycleTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val catalogFile get() = File(context.filesDir, "legado-book-sources.json")

    @Before
    fun resetCatalog() {
        catalogFile.delete()
        File(catalogFile.path + ".bak").delete()
        File(catalogFile.path + ".new").delete()
    }

    @Test
    fun removeInvalidatesOnlyAfterCatalogCommitAndOnlyWhenSourceExists() = runBlocking {
        val invalidated = mutableListOf<String>()
        val catalog = FileBookSourceCatalog(context) { sourceId ->
            assertFalse(catalogFile.readText().contains(sourceId))
            invalidated += sourceId
        }
        assertTrue(catalog.importJson(source("Before", "https://remove-life.example")) is EngineResult.Success)

        val removed = catalog.remove("https://remove-life.example")
        assertEquals(true, (removed as EngineResult.Success).value)
        assertEquals(listOf("https://remove-life.example"), invalidated)

        val absent = catalog.remove("https://missing-life.example")
        assertEquals(false, (absent as EngineResult.Success).value)
        assertEquals(listOf("https://remove-life.example"), invalidated)
    }

    @Test
    fun importingOnlyInvalidatesChangedExistingIdsNotNewOrUnchangedSources() = runBlocking {
        val invalidated = mutableListOf<String>()
        val catalog = FileBookSourceCatalog(context) { sourceId -> invalidated += sourceId }
        val first = "https://replace-life.example"
        val unchanged = "https://unchanged-life.example"
        assertTrue(catalog.importJson(
            """[${source("Old", first)}, ${source("Stable", unchanged)}]"""
        ) is EngineResult.Success)

        val result = catalog.importJson(
            """[${source("New", first)}, ${source("Stable", unchanged)}, ${source("Brand new", "https://new-life.example")}]"""
        )

        assertEquals(3, (result as EngineResult.Success).value)
        assertEquals(listOf(first), invalidated)
        assertTrue(catalogFile.readText().contains("New"))
    }

    @Test
    fun malformedImportDoesNotInvalidateAnyRuntimeState() = runBlocking {
        val invalidated = mutableListOf<String>()
        val catalog = FileBookSourceCatalog(context) { sourceId -> invalidated += sourceId }

        val result = catalog.importJson("""{"bookSourceName":"missing id"}""")

        assertTrue(result is EngineResult.Failure)
        assertTrue(invalidated.isEmpty())
        assertFalse(catalogFile.exists())
    }

    @Test
    fun cleanupFailureIsReportedAfterTheCatalogMutationHasCommitted() = runBlocking {
        val sourceId = "https://cleanup-failure.example"
        val catalog = FileBookSourceCatalog(context) {
            throw IllegalStateException("cleanup unavailable")
        }
        assertTrue(catalog.importJson(source("Before", sourceId)) is EngineResult.Success)

        val result = catalog.remove(sourceId)

        assertTrue(result is EngineResult.Failure)
        assertFalse(catalogFile.readText().contains(sourceId))
    }

    private fun source(name: String, id: String): String =
        """{"bookSourceUrl":"$id","bookSourceName":"$name"}"""
}

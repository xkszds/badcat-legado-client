package app.badcat.client

import android.content.Context
import android.util.AtomicFile
import app.badcat.engine.BookSource
import app.badcat.engine.BookSourceCatalog
import app.badcat.engine.EngineErrorCode
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoBookSource
import app.badcat.engine.LegadoBookSourceCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException

private val catalogFileMutex = Mutex()

/**
 * A small, durable host-side catalog for imported Legado source descriptors.
 *
 * The complete upstream JSON object is retained by [LegadoBookSourceCodec], so
 * fields unknown to this host survive import/export. This class stores metadata;
 * it does not execute source JavaScript or claim full Legado compatibility.
 */
class FileBookSourceCatalog(
    context: Context,
    private val onSourceStateInvalidated: suspend (sourceId: String) -> Unit = {},
) : BookSourceCatalog {
    private val file = AtomicFile(File(context.applicationContext.filesDir, "legado-book-sources.json"))
    // Activities create separate catalog instances; serialize access to the shared file process-wide.
    private val mutex = catalogFileMutex

    override suspend fun all(): EngineResult<List<BookSource>> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                orderBookSourcesForCatalog(readSources())
                    .map { source -> source.toEngineSource() }
                    .let { EngineResult.Success(it) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                EngineResult.Failure(
                    EngineErrorCode.UNKNOWN,
                    "Unable to read the local book-source catalog",
                    error,
                )
            }
        }
    }

    /** Import one Legado source object or an array; existing IDs are replaced in place. */
    suspend fun importJson(json: String): EngineResult<Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val imported = try {
                LegadoBookSourceCodec.decode(json)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                return@withLock EngineResult.Failure(
                    EngineErrorCode.INVALID_SOURCE,
                    error.message ?: "Invalid Legado book-source JSON",
                    error,
                )
            }

            try {
                val existing = readSources().associateBy { it.sourceId }
                val replacedIds = imported
                    .filter { incoming -> existing[incoming.sourceId]?.let { it != incoming } == true }
                    .map { it.sourceId }
                val merged = LinkedHashMap<String, LegadoBookSource>()
                existing.values.forEach { source -> merged[source.sourceId] = source }
                imported.forEach { source -> merged[source.sourceId] = source }
                writeSources(merged.values.toList())
                val cleanupFailures = invalidateRuntimeSources(replacedIds)
                if (cleanupFailures.isNotEmpty()) {
                    return@withLock EngineResult.Failure(
                        EngineErrorCode.UNKNOWN,
                        "Book sources were imported, but runtime state cleanup failed for: ${cleanupFailures.joinToString()}",
                    )
                }
                EngineResult.Success(imported.size)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                EngineResult.Failure(
                    EngineErrorCode.UNKNOWN,
                    "Unable to save imported book sources",
                    error,
                )
            }
        }
    }

    /** Persist the enabled flag in the original JSON while retaining all other source fields. */
    suspend fun setEnabled(sourceId: String, enabled: Boolean): EngineResult<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val sources = readSources().toMutableList()
                    val index = sources.indexOfFirst { it.sourceId == sourceId }
                    if (index < 0) {
                        return@withLock EngineResult.Failure(
                            EngineErrorCode.NOT_FOUND,
                            "Book source '$sourceId' was not found",
                        )
                    }
                    sources[index] = LegadoBookSourceCodec.withEnabled(sources[index], enabled)
                    writeSources(sources)
                    EngineResult.Success(Unit)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    EngineResult.Failure(
                        EngineErrorCode.UNKNOWN,
                        "Unable to update book-source enabled state",
                        error,
                    )
                }
            }
        }

    /** Remove one source while preserving all unrelated source records. */
    suspend fun remove(sourceId: String): EngineResult<Boolean> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val sources = readSources()
                val remaining = sources.filterNot { it.sourceId == sourceId }
                if (remaining.size == sources.size) {
                    return@withLock EngineResult.Success(false)
                }
                writeSources(remaining)
                val cleanupFailures = invalidateRuntimeSources(listOf(sourceId))
                if (cleanupFailures.isNotEmpty()) {
                    return@withLock EngineResult.Failure(
                        EngineErrorCode.UNKNOWN,
                        "Book source was removed, but runtime state cleanup failed for: ${cleanupFailures.joinToString()}",
                    )
                }
                EngineResult.Success(true)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                EngineResult.Failure(
                    EngineErrorCode.UNKNOWN,
                    "Unable to remove the book source",
                    error,
                )
            }
        }
    }

    /** Export the catalog as a Legado-compatible JSON array. */
    suspend fun exportJson(): EngineResult<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                EngineResult.Success(
                    LegadoBookSourceCodec.encode(orderBookSourcesForCatalog(readSources()))
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                EngineResult.Failure(
                    EngineErrorCode.UNKNOWN,
                    "Unable to export the local book-source catalog",
                    error,
                )
            }
        }
    }

    private suspend fun invalidateRuntimeSources(sourceIds: List<String>): List<String> {
        val failures = mutableListOf<String>()
        sourceIds.distinct().forEach { sourceId ->
            try {
                onSourceStateInvalidated(sourceId)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                failures += sourceId
            }
        }
        return failures
    }

    private fun readSources(): List<LegadoBookSource> {
        // Always let AtomicFile inspect/restore its backup before treating the catalog as absent.
        val input = try {
            file.openRead()
        } catch (error: FileNotFoundException) {
            val backupFile = File(file.baseFile.path + ".bak")
            if (!file.baseFile.exists() && !backupFile.exists()) return emptyList()
            throw error
        }
        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size() + count <= LegadoBookSourceCodec.MAX_IMPORT_BYTES) {
                    "Local book-source catalog exceeds the 5 MiB limit"
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val json = LegadoBookSourceCodec.decodeUtf8ToString(bytes)
        return if (json.isBlank()) emptyList() else LegadoBookSourceCodec.decode(json)
    }

    private fun writeSources(sources: List<LegadoBookSource>) {
        val bytes = LegadoBookSourceCodec.encode(sources).toByteArray(Charsets.UTF_8)
        require(bytes.size <= LegadoBookSourceCodec.MAX_IMPORT_BYTES) {
            "Local book-source catalog exceeds the 5 MiB limit"
        }
        val output = file.startWrite()
        try {
            output.write(bytes)
            output.flush()
            file.finishWrite(output)
        } catch (error: Exception) {
            // Always close/roll back AtomicFile, including when the coroutine is cancelled.
            runCatching { file.failWrite(output) }
            if (error is CancellationException) throw error
            throw error
        }
    }
}


/** Matches the pinned Legado BookSourceDao.all() customOrder ASC query. */
internal fun orderBookSourcesForCatalog(
    sources: List<LegadoBookSource>,
): List<LegadoBookSource> = sources.sortedBy { it.customOrder }

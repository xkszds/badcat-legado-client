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
import java.io.File

private val catalogFileMutex = Mutex()

/**
 * A small, durable host-side catalog for imported Legado source descriptors.
 *
 * The complete upstream JSON object is retained by [LegadoBookSourceCodec], so
 * fields unknown to this host survive import/export. This class stores metadata;
 * it does not execute source JavaScript or claim full Legado compatibility.
 */
class FileBookSourceCatalog(context: Context) : BookSourceCatalog {
    private val file = AtomicFile(File(context.applicationContext.filesDir, "legado-book-sources.json"))
    // Activities create separate catalog instances; serialize access to the shared file process-wide.
    private val mutex = catalogFileMutex

    override suspend fun all(): EngineResult<List<BookSource>> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                readSources().map { source -> source.toEngineSource() }
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
                val merged = LinkedHashMap<String, LegadoBookSource>()
                readSources().forEach { source -> merged[source.sourceId] = source }
                imported.forEach { source -> merged[source.sourceId] = source }
                writeSources(merged.values.toList())
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

    /** Export the catalog as a Legado-compatible JSON array. */
    suspend fun exportJson(): EngineResult<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                EngineResult.Success(LegadoBookSourceCodec.encode(readSources()))
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

    private fun readSources(): List<LegadoBookSource> {
        if (!file.baseFile.exists()) return emptyList()
        return file.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
            val json = reader.readText()
            if (json.isBlank()) emptyList() else LegadoBookSourceCodec.decode(json)
        }
    }

    private fun writeSources(sources: List<LegadoBookSource>) {
        val output = file.startWrite()
        try {
            val writer = output.writer(Charsets.UTF_8)
            writer.write(LegadoBookSourceCodec.encode(sources))
            writer.flush()
            file.finishWrite(output)
        } catch (error: Exception) {
            // Always close/roll back AtomicFile, including when the coroutine is cancelled.
            runCatching { file.failWrite(output) }
            if (error is CancellationException) throw error
            throw error
        }
    }
}

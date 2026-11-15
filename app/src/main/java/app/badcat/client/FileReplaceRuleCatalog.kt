package app.badcat.client

import android.content.Context
import android.util.AtomicFile
import app.badcat.engine.EngineErrorCode
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoReplaceRuleCodec
import app.badcat.engine.LegadoReplaceRuleRecord
import app.badcat.engine.LegadoReplaceRuleCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private val replaceRuleCatalogMutex = Mutex()

/**
 * Durable app-private storage for imported Legado replacement rules.
 *
 * The catalog implements the engine's read contract; literal rules are applied
 * during chapter loading. JavaScript replacement expressions remain fail-closed.
 */
class FileReplaceRuleCatalog(context: Context) : LegadoReplaceRuleCatalog {
    private val file = AtomicFile(
        File(context.applicationContext.filesDir, "legado-replace-rules.json")
    )
    private val mutex = replaceRuleCatalogMutex

    override suspend fun all(): EngineResult<List<LegadoReplaceRuleRecord>> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    EngineResult.Success(readRules())
                } catch (error: Exception) {
                    EngineResult.Failure(
                        EngineErrorCode.UNKNOWN,
                        "Unable to read the local replacement-rule catalog",
                        error,
                    )
                }
            }
        }

    /** Import one Legado rule object or an array; existing IDs are replaced in place. */
    suspend fun importJson(json: String): EngineResult<Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val imported = try {
                LegadoReplaceRuleCodec.decode(json).also { rules ->
                    val invalid = rules.firstOrNull { !it.isValid() }
                    require(invalid == null) {
                        "Invalid Legado replacement rule '${invalid?.scopeRule?.id}'"
                    }
                }
            } catch (error: Exception) {
                return@withLock EngineResult.Failure(
                    EngineErrorCode.INVALID_SOURCE,
                    error.message ?: "Invalid Legado replacement-rule JSON",
                    error,
                )
            }

            try {
                val merged = LinkedHashMap<Long, LegadoReplaceRuleRecord>()
                readRules().forEach { rule -> merged[rule.scopeRule.id] = rule }
                imported.forEach { rule -> merged[rule.scopeRule.id] = rule }
                writeRules(merged.values.toList())
                EngineResult.Success(imported.size)
            } catch (error: Exception) {
                EngineResult.Failure(
                    EngineErrorCode.UNKNOWN,
                    "Unable to save imported replacement rules",
                    error,
                )
            }
        }
    }

    override suspend fun setEnabled(ruleId: Long, enabled: Boolean): EngineResult<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val rules = readRules().toMutableList()
                    val index = rules.indexOfFirst { it.scopeRule.id == ruleId }
                    if (index < 0) {
                        return@withLock EngineResult.Failure(
                            EngineErrorCode.NOT_FOUND,
                            "Replacement rule '$ruleId' was not found",
                        )
                    }
                    rules[index] = LegadoReplaceRuleCodec.withEnabled(rules[index], enabled)
                    writeRules(rules)
                    EngineResult.Success(Unit)
                } catch (error: Exception) {
                    EngineResult.Failure(
                        EngineErrorCode.UNKNOWN,
                        "Unable to update replacement-rule enabled state",
                        error,
                    )
                }
            }
        }

    suspend fun exportJson(): EngineResult<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                EngineResult.Success(LegadoReplaceRuleCodec.encode(readRules()))
            } catch (error: Exception) {
                EngineResult.Failure(
                    EngineErrorCode.UNKNOWN,
                    "Unable to export the local replacement-rule catalog",
                    error,
                )
            }
        }
    }

    private fun readRules(): List<LegadoReplaceRuleRecord> {
        val backupFile = File(file.baseFile.path + ".bak")
        if (!file.baseFile.exists() && !backupFile.exists()) return emptyList()
        return file.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
            val json = reader.readText()
            if (json.isBlank()) emptyList() else LegadoReplaceRuleCodec.decode(json)
        }
    }

    private fun writeRules(rules: List<LegadoReplaceRuleRecord>) {
        val output = file.startWrite()
        try {
            val writer = output.writer(Charsets.UTF_8)
            writer.write(LegadoReplaceRuleCodec.encode(rules))
            writer.flush()
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}

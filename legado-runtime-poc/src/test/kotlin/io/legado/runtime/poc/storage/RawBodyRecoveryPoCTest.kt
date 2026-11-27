package io.legado.runtime.poc.storage

import android.util.AtomicFile
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.security.MessageDigest

private data class LegacyToken(val bookUrl: String, val chapterIndex: Int, val folderName: String, val version: Long)
private data class BodyMetadata(val bookUrl: String, val chapterIndex: Int, val title: String, val bodySha256: String)
private data class PendingBodyOperation(
    val operationId: String,
    val body: String,
    val metadata: BodyMetadata,
    val state: String,
)

private class LegacyProcessFence {
    private val versions = mutableMapOf<Pair<String, Int>, Long>()
    @Synchronized fun capture(bookUrl: String, index: Int, folder: String) =
        LegacyToken(bookUrl, index, folder, versions[bookUrl to index] ?: 0L)
    @Synchronized fun isCurrent(token: LegacyToken) =
        (versions[token.bookUrl to token.chapterIndex] ?: 0L) == token.version
    @Synchronized fun replace(bookUrl: String, index: Int) {
        versions[bookUrl to index] = (versions[bookUrl to index] ?: 0L) + 1L
    }
}

/** Behavioral model derived from pinned BookHelp; it is not execution of upstream Android code. */
private class LegacyBookHelpModel(private val root: File, private val fence: LegacyProcessFence = LegacyProcessFence()) {
    private val gson = Gson()
    private val bodyFile = File(root, "book_cache/book-A/00000-title.nb")
    private val metadataFile = File(root, "chapter-metadata.json")

    fun token() = fence.capture("book-A", 0, "book-A")
    fun seed(body: String, metadata: BodyMetadata) {
        bodyFile.parentFile!!.mkdirs()
        bodyFile.writeText(body)
        metadataFile.writeText(gson.toJson(metadata))
    }
    fun body(): String? = bodyFile.takeIf(File::isFile)?.readText()
    fun metadata(): BodyMetadata? = metadataFile.takeIf(File::isFile)?.readText()?.let { gson.fromJson(it, BodyMetadata::class.java) }

    fun save(token: LegacyToken, body: String, metadata: BodyMetadata, crashAfterBody: Boolean = false) {
        require(token.bookUrl == "book-A" && token.chapterIndex == 0 && token.folderName == "book-A")
        if (!fence.isCurrent(token)) return
        bodyFile.parentFile!!.mkdirs()
        bodyFile.writeText(body) // mirrors BookHelp.writeText: ordinary File.writeText, not AtomicFile
        if (crashAfterBody) throw RawBodySimulatedProcessDeath()
        metadataFile.writeText(gson.toJson(metadata)) // separate owner; not atomic with body
    }
    fun partialOverwriteAndCrash(prefix: String) {
        bodyFile.parentFile!!.mkdirs()
        FileOutputStream(bodyFile, false).use { it.write(prefix.toByteArray()) }
        throw RawBodySimulatedProcessDeath()
    }
    fun authoritativeReplace() = fence.replace("book-A", 0)
    fun clearBookCache() {
        File(root, "book_cache/book-A").deleteRecursively() // mirrors clearCache(book): no fence advance
    }
    fun restart(): LegacyBookHelpModel = LegacyBookHelpModel(root, LegacyProcessFence())
    fun currentVersion() = token().version
}

private class RecoverableBodyStore(private val root: File) {
    private val gson = Gson()
    private val body = AtomicFile(File(root, "raw-body.txt"))
    private val metadata = AtomicFile(File(root, "chapter-metadata.json"))
    private val journal = AtomicFile(File(root, "body-operation.json"))

    init { root.mkdirs() }

    private fun read(file: AtomicFile): String? = try {
        file.openRead().bufferedReader().use { it.readText() }
    } catch (_: FileNotFoundException) { null }

    private fun write(file: AtomicFile, value: String) {
        val stream = file.startWrite()
        try {
            stream.write(value.toByteArray(Charsets.UTF_8))
            stream.flush()
            file.finishWrite(stream)
        } catch (failure: Throwable) {
            file.failWrite(stream)
            throw failure
        }
    }

    fun body(): String? = read(body)
    fun metadata(): BodyMetadata? = read(metadata)?.let { gson.fromJson(it, BodyMetadata::class.java) }
    fun journal(): PendingBodyOperation? = read(journal)?.let { gson.fromJson(it, PendingBodyOperation::class.java) }

    fun apply(operationId: String, bodyText: String, meta: BodyMetadata, crashAfterBody: Boolean = false, crashAfterMetadata: Boolean = false) {
        val old = journal()
        if (old != null && old.operationId == operationId && old.state == "COMMITTED") return
        require(old == null || old.operationId == operationId) { "another operation is pending" }
        val pending = PendingBodyOperation(operationId, bodyText, meta, "PREPARED")
        write(journal, gson.toJson(pending))
        write(body, bodyText)
        if (crashAfterBody) throw RawBodySimulatedProcessDeath()
        write(metadata, gson.toJson(meta))
        if (crashAfterMetadata) throw RawBodySimulatedProcessDeath()
        write(journal, gson.toJson(pending.copy(state = "COMMITTED")))
    }

    fun replayPending() {
        val pending = journal() ?: return
        if (pending.state == "COMMITTED") return
        write(body, pending.body)
        write(metadata, gson.toJson(pending.metadata))
        write(journal, gson.toJson(pending.copy(state = "COMMITTED")))
    }

    fun bodyHash(): String? = body()?.let(::sha256)
    fun metadataHash(): String? = metadata()?.let { sha256(gson.toJson(it)) }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

private class RawBodySimulatedProcessDeath : RuntimeException("simulated process death")

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RawBodyRecoveryPoCTest {
    private fun tempDir(): File {
        val dir = File.createTempFile("raw-body-poc", "dir")
        check(dir.delete() && dir.mkdirs())
        return dir
    }

    private fun meta(title: String, body: String) =
        BodyMetadata("book-A", 0, title, MessageDigest.getInstance("SHA-256")
            .digest(body.toByteArray()).joinToString("") { "%02x".format(it) })

    @Test fun legacyPlainOverwriteCanLeaveTruncatedBodyWithOldMetadata() {
        val dir = tempDir()
        try {
            val legacy = LegacyBookHelpModel(dir)
            val old = "old complete body"
            legacy.seed(old, meta("old-title", old))
            try { legacy.partialOverwriteAndCrash("new-") } catch (_: RawBodySimulatedProcessDeath) { }
            assertEquals("new-", legacy.body())
            assertEquals("old-title", legacy.metadata()!!.title)
            assertNotEquals(legacy.metadata()!!.bodySha256, meta("new-", "new-").bodySha256)
        } finally { dir.deleteRecursively() }
    }

    @Test fun legacyCrashAfterBodyWriteLeavesBodyAndMetadataOutOfSync() {
        val dir = tempDir()
        try {
            val legacy = LegacyBookHelpModel(dir)
            val old = "old body"
            legacy.seed(old, meta("old", old))
            val next = "new body"
            try { legacy.save(legacy.token(), next, meta("new", next), crashAfterBody = true) }
            catch (_: RawBodySimulatedProcessDeath) { }
            assertEquals(next, legacy.body())
            assertEquals("old", legacy.metadata()!!.title)
            assertNotEquals(legacy.metadata()!!.bodySha256, meta("new", next).bodySha256)
        } finally { dir.deleteRecursively() }
    }

    @Test fun legacyClearBookCacheDoesNotInvalidateAnInflightToken() {
        val dir = tempDir()
        try {
            val legacy = LegacyBookHelpModel(dir)
            val old = "old body"
            legacy.seed(old, meta("old", old))
            val token = legacy.token()
            legacy.clearBookCache()
            assertNull(legacy.body())
            val next = "late stale response"
            legacy.save(token, next, meta("late", next))
            assertEquals("late stale response", legacy.body())
        } finally { dir.deleteRecursively() }
    }

    @Test fun legacyFenceGenerationIsProcessLocalAndResetsOnRestart() {
        val dir = tempDir()
        try {
            val legacy = LegacyBookHelpModel(dir)
            legacy.seed("body", meta("title", "body"))
            assertEquals(0L, legacy.currentVersion())
            // An authoritative replacement advances the in-memory generation.
            legacy.authoritativeReplace()
            assertEquals(1L, legacy.currentVersion())
            val restarted = legacy.restart()
            assertEquals(0L, restarted.currentVersion())
        } finally { dir.deleteRecursively() }
    }

    @Test fun journalReplaysBodyAndMetadataAfterCrashBetweenTheirWrites() {
        val dir = tempDir()
        try {
            val first = RecoverableBodyStore(dir)
            val next = "complete replacement body"
            try { first.apply("op-1", next, meta("replacement", next), crashAfterBody = true) }
            catch (_: RawBodySimulatedProcessDeath) { }
            assertEquals(next, first.body())
            assertNull(first.metadata())
            val restarted = RecoverableBodyStore(dir)
            restarted.replayPending()
            assertEquals(next, restarted.body())
            assertEquals(meta("replacement", next), restarted.metadata())
            assertEquals("COMMITTED", restarted.journal()!!.state)
            val bodyHash = restarted.bodyHash()
            val metadataHash = restarted.metadataHash()
            restarted.replayPending()
            assertEquals(bodyHash, restarted.bodyHash())
            assertEquals(metadataHash, restarted.metadataHash())
        } finally { dir.deleteRecursively() }
    }

    @Test fun journalReplayIsIdempotentAfterMetadataWriteBeforeCommitMarker() {
        val dir = tempDir()
        try {
            val store = RecoverableBodyStore(dir)
            val next = "body committed before journal marker"
            try { store.apply("op-2", next, meta("title-2", next), crashAfterMetadata = true) }
            catch (_: RawBodySimulatedProcessDeath) { }
            assertEquals(next, store.body())
            assertEquals(meta("title-2", next), store.metadata())
            assertEquals("PREPARED", store.journal()!!.state)
            val restarted = RecoverableBodyStore(dir)
            restarted.replayPending()
            assertEquals("COMMITTED", restarted.journal()!!.state)
            val hash = restarted.bodyHash()
            restarted.replayPending()
            assertEquals(hash, restarted.bodyHash())
        } finally { dir.deleteRecursively() }
    }

    @Test fun atomicBodyWriteFailureRetainsPreviousCommittedBody() {
        val dir = tempDir()
        try {
            val atomic = AtomicFile(File(dir, "body.txt"))
            fun write(value: String) {
                val stream = atomic.startWrite()
                try {
                    stream.write(value.toByteArray())
                    atomic.finishWrite(stream)
                } catch (error: Throwable) {
                    atomic.failWrite(stream)
                    throw error
                }
            }
            write("old committed")
            val stream = atomic.startWrite()
            stream.write("partial".toByteArray())
            atomic.failWrite(stream)
            assertEquals("old committed", atomic.openRead().bufferedReader().use { it.readText() })
        } finally { dir.deleteRecursively() }
    }
}

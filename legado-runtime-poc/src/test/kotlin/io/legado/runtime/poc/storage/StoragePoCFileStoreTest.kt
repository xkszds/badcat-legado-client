package io.legado.runtime.poc.storage

import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.concurrent.Executors

private data class FileBookRow(val bookUrl: String, val title: String, val rawJson: String)
private data class FileChapterRow(val url: String, val bookUrl: String, val chapterIndex: Int, val title: String, val rawContentRef: String)
private data class FileSnapshot(
    val books: MutableList<FileBookRow> = mutableListOf(),
    val chapters: MutableList<FileChapterRow> = mutableListOf()
)
private data class JournalRow(
    val operationId: String,
    val book: FileBookRow,
    val chapter: FileChapterRow,
    var state: String
)
private data class FixtureBundle(val book: FileBookRow, val chapter: FileChapterRow)
private enum class CrashAt { AFTER_PREPARED, AFTER_SNAPSHOT }
private class SimulatedProcessDeath : RuntimeException()

private class AtomicBytes(file: File) {
    private val atomic = AtomicFile(file)
    private val base = file

    fun read(): ByteArray? = try {
        atomic.openRead().use { it.readBytes() }
    } catch (missing: FileNotFoundException) {
        if (!base.exists() && !File(base.path + ".bak").exists()) null else throw missing
    }

    fun delete() = atomic.delete()

    fun write(bytes: ByteArray) {
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (failure: Throwable) {
            atomic.failWrite(stream)
            throw failure
        }
    }
}

/** Test-only candidate: one AtomicFile metadata snapshot plus a durable replay journal. */
private class FileJournalStore(private val directory: File) {
    private val gson = Gson()
    private val snapshotFile = AtomicBytes(File(directory, "metadata.json"))
    private val journalFile = AtomicBytes(File(directory, "operations.json"))
    private val rawContentFile = AtomicBytes(File(directory, "raw-content.bin"))
    private val parsedCacheFile = AtomicBytes(File(directory, "parsed-cache.json"))

    init { directory.mkdirs() }

    private fun snapshot(): FileSnapshot {
        val bytes = snapshotFile.read() ?: return FileSnapshot()
        val root = JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
        val bookType = object : TypeToken<MutableList<FileBookRow>>() {}.type
        val chapterType = object : TypeToken<MutableList<FileChapterRow>>() {}.type
        val books = root.get("books")?.takeUnless { it.isJsonNull }
            ?.let { gson.fromJson<MutableList<FileBookRow>>(it, bookType) } ?: mutableListOf()
        val chapters = root.get("chapters")?.takeUnless { it.isJsonNull }
            ?.let { gson.fromJson<MutableList<FileChapterRow>>(it, chapterType) } ?: mutableListOf()
        return FileSnapshot(books, chapters)
    }

    private fun journal(): MutableList<JournalRow> = journalFile.read()?.let {
        val type = object : TypeToken<MutableList<JournalRow>>() {}.type
        gson.fromJson<MutableList<JournalRow>>(String(it, Charsets.UTF_8), type) ?: mutableListOf()
    } ?: mutableListOf()

    private fun saveSnapshot(value: FileSnapshot) = snapshotFile.write(gson.toJson(value).toByteArray())
    private fun saveJournal(value: List<JournalRow>) = journalFile.write(gson.toJson(value).toByteArray())

    @Synchronized fun apply(bundle: FixtureBundle, operationId: String, crashAt: CrashAt? = null) {
        val rows = journal()
        val existing = rows.firstOrNull { it.operationId == operationId }
        if (existing?.state == "COMMITTED") return
        if (existing != null && (existing.book != bundle.book || existing.chapter != bundle.chapter)) {
            error("operationId reused with different payload")
        }
        val entry = existing ?: JournalRow(operationId, bundle.book, bundle.chapter, "PREPARED").also {
            rows.add(it)
            saveJournal(rows)
        }
        if (crashAt == CrashAt.AFTER_PREPARED) throw SimulatedProcessDeath()
        applyToSnapshot(bundle)
        if (crashAt == CrashAt.AFTER_SNAPSHOT) throw SimulatedProcessDeath()
        entry.state = "COMMITTED"
        saveJournal(rows)
    }

    private fun applyToSnapshot(bundle: FixtureBundle) {
        val current = snapshot()
        val oldBook = current.books.firstOrNull { it.bookUrl == bundle.book.bookUrl }
        if (oldBook == null) current.books.add(bundle.book)
        else require(oldBook == bundle.book) { "Book identity has conflicting payload: ${bundle.book.bookUrl}" }

        val oldChapter = current.chapters.firstOrNull {
            it.url == bundle.chapter.url && it.bookUrl == bundle.chapter.bookUrl
        }
        if (oldChapter != null) {
            require(oldChapter == bundle.chapter) { "(url,bookUrl) primary-key conflict" }
        } else {
            val indexOwner = current.chapters.firstOrNull {
                it.bookUrl == bundle.chapter.bookUrl && it.chapterIndex == bundle.chapter.chapterIndex
            }
            require(indexOwner == null) { "(bookUrl,index) unique-index conflict" }
            current.chapters.add(bundle.chapter)
        }
        current.books.sortBy { it.bookUrl }
        current.chapters.sortWith(compareBy<FileChapterRow>({ it.bookUrl }, { it.chapterIndex }))
        saveSnapshot(current)
    }

    @Synchronized fun replayPending() {
        val rows = journal()
        rows.filter { it.state == "PREPARED" }.forEach { row ->
            applyToSnapshot(FixtureBundle(row.book, row.chapter))
            row.state = "COMMITTED"
            saveJournal(rows)
        }
    }

    @Synchronized fun seedToc(book: FileBookRow, chapters: List<FileChapterRow>) {
        val current = snapshot()
        val existing = current.books.firstOrNull { it.bookUrl == book.bookUrl }
        require(existing == null || existing == book) { "Book fixture conflict" }
        if (existing == null) current.books.add(book)
        val keys = current.chapters.map { it.url to it.bookUrl }.toMutableSet()
        val indexes = current.chapters.map { it.bookUrl to it.chapterIndex }.toMutableSet()
        chapters.forEach { chapter ->
            require(keys.add(chapter.url to chapter.bookUrl)) { "duplicate chapter primary key" }
            require(indexes.add(chapter.bookUrl to chapter.chapterIndex)) { "duplicate chapter index" }
        }
        current.chapters.addAll(chapters)
        current.books.sortBy { it.bookUrl }
        current.chapters.sortWith(compareBy<FileChapterRow>({ it.bookUrl }, { it.chapterIndex }))
        saveSnapshot(current)
    }

    fun chapterIndices(bookUrl: String): List<Int> = snapshot().chapters
        .filter { it.bookUrl == bookUrl }.map { it.chapterIndex }
    fun bookRawJson(bookUrl: String): String? = snapshot().books.firstOrNull { it.bookUrl == bookUrl }?.rawJson

    @Synchronized fun dryRun(bundles: List<FixtureBundle>): Pair<Int, Int> {
        val original = snapshot()
        val probe = FileSnapshot(original.books.toMutableList(), original.chapters.toMutableList())
        bundles.forEach { bundle ->
            val oldBook = probe.books.firstOrNull { it.bookUrl == bundle.book.bookUrl }
            require(oldBook == null || oldBook == bundle.book) { "Book conflict" }
            if (oldBook == null) probe.books.add(bundle.book)
            val oldChapter = probe.chapters.firstOrNull { it.url == bundle.chapter.url && it.bookUrl == bundle.chapter.bookUrl }
            if (oldChapter == null) {
                require(probe.chapters.none { it.bookUrl == bundle.chapter.bookUrl && it.chapterIndex == bundle.chapter.chapterIndex }) { "chapter index conflict" }
                probe.chapters.add(bundle.chapter)
            } else require(oldChapter == bundle.chapter) { "chapter primary-key conflict" }
        }
        return (probe.books.size - original.books.size) to (probe.chapters.size - original.chapters.size)
    }

    @Synchronized fun migrate(bundles: List<FixtureBundle>) {
        bundles.forEach { apply(it, "migration:${it.book.bookUrl}:${it.chapter.chapterIndex}") }
    }

    fun snapshotBytes(): ByteArray = snapshotFile.read() ?: "{}".toByteArray()
    fun backupState(): Pair<ByteArray?, ByteArray?> = snapshotFile.read() to journalFile.read()
    fun restoreState(backup: Pair<ByteArray?, ByteArray?>) {
        if (backup.first == null) snapshotFile.delete() else snapshotFile.write(backup.first!!)
        if (backup.second == null) journalFile.delete() else journalFile.write(backup.second!!)
    }
    fun snapshotCounts(): Pair<Int, Int> = snapshot().let { it.books.size to it.chapters.size }
    fun snapshotHash(): String = sha256(snapshotBytes())
    fun stateHash(): String = sha256((snapshotFile.read() ?: byteArrayOf()) + byteArrayOf(0) + (journalFile.read() ?: byteArrayOf()))
    fun journalStates(): List<String> = journal().map { it.state }
    fun writeRawContent(bytes: ByteArray) = rawContentFile.write(bytes)
    fun writeParsedCache(bytes: ByteArray) = parsedCacheFile.write(bytes)
    fun rawContentBytes(): ByteArray? = rawContentFile.read()
    fun parsedCacheBytes(): ByteArray? = parsedCacheFile.read()

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}

private class FixtureSaveFence {
    private val versions = mutableMapOf<String, Long>()
    @Synchronized fun capture(key: String): Long = versions[key] ?: 0L
    @Synchronized fun invalidate(key: String) { versions[key] = (versions[key] ?: 0L) + 1L }
    @Synchronized fun mayPublish(key: String, token: Long): Boolean = capture(key) == token
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StoragePoCFileStoreTest {
    private fun store(): Pair<File, FileJournalStore> {
        val dir = File.createTempFile("storage-poc", "dir")
        check(dir.delete() && dir.mkdirs())
        return dir to FileJournalStore(dir)
    }

    private fun bundle(book: String, url: String, index: Int, raw: String = "{}"): FixtureBundle =
        FixtureBundle(
            FileBookRow(book, "title-$book", raw),
            FileChapterRow(url, book, index, "chapter-$index", "raw/$book/$index")
        )

    @Test fun journalReplaysAfterBothCrashWindowsAndIsIdempotent() {
        val (dir, store) = store()
        try {
            val first = bundle("book-A", "url-0", 0, "{\"futureField\":9}")
            try { store.apply(first, "op-1", CrashAt.AFTER_PREPARED); fail("simulated death expected") }
            catch (_: SimulatedProcessDeath) { }
            assertEquals(listOf("PREPARED"), store.journalStates())
            store.replayPending()
            assertEquals(1 to 1, store.snapshotCounts())
            assertEquals(listOf("COMMITTED"), store.journalStates())
            store.apply(first, "op-1")
            assertEquals(1 to 1, store.snapshotCounts())

            val second = bundle("book-B", "url-1", 0)
            try { store.apply(second, "op-2", CrashAt.AFTER_SNAPSHOT); fail("simulated death expected") }
            catch (_: SimulatedProcessDeath) { }
            assertEquals(2 to 2, store.snapshotCounts())
            store.replayPending()
            assertEquals(2 to 2, store.snapshotCounts())
            assertEquals(listOf("COMMITTED", "COMMITTED"), store.journalStates())
        } finally { dir.deleteRecursively() }
    }

    @Test fun compositePrimaryKeyAndBookIndexUniquenessAreEnforced() {
        val (dir, store) = store()
        try {
            store.apply(bundle("book-A", "same-url", 0), "a")
            store.apply(bundle("book-B", "same-url", 0), "b")
            assertEquals(2 to 2, store.snapshotCounts())
            try {
                store.apply(bundle("book-A", "same-url", 1), "same-pk")
                fail("expected (url,bookUrl) conflict")
            } catch (_: IllegalArgumentException) { }
            try {
                store.apply(bundle("book-A", "other-url", 0), "same-index")
                fail("expected (bookUrl,index) conflict")
            } catch (_: IllegalArgumentException) { }
            assertEquals(2 to 2, store.snapshotCounts())
        } finally { dir.deleteRecursively() }
    }

    @Test fun dryRunMigrationAndRollbackPreserveFixtureHashes() {
        val (dir, store) = store()
        try {
            val before = store.stateHash()
            val fixtures = listOf(bundle("book-A", "u0", 0, "{\"nullableField\":null,\"unknown\":\"retained\"}"), bundle("book-B", "u0", 0))
            assertEquals(2 to 2, store.dryRun(fixtures))
            assertEquals(before, store.stateHash())
            val backup = store.backupState()
            store.migrate(fixtures)
            assertEquals(2 to 2, store.snapshotCounts())
            assertEquals("{\"nullableField\":null,\"unknown\":\"retained\"}", store.bookRawJson("book-A"))
            val migratedHash = store.stateHash()
            store.migrate(fixtures)
            assertEquals(migratedHash, store.stateHash())
            store.restoreState(backup)
            assertEquals(before, store.stateHash())
            assertEquals(0 to 0, store.snapshotCounts())
        } finally { dir.deleteRecursively() }
    }

    @Test fun rawContentParsedCacheAndSaveFenceRemainSeparate() {
        val (dir, store) = store()
        try {
            val fence = FixtureSaveFence()
            val key = "book-A#0"
            val oldToken = fence.capture(key)
            fence.invalidate(key)
            assertFalse(fence.mayPublish(key, oldToken))
            val currentToken = fence.capture(key)
            assertTrue(fence.mayPublish(key, currentToken))
            val raw = ByteArray(512 * 1024) { (it % 251).toByte() }
            val parsed = "{\"parsed\":true}".toByteArray()
            store.writeRawContent(raw)
            store.writeParsedCache(parsed)
            assertArrayEquals(raw, store.rawContentBytes())
            assertArrayEquals(parsed, store.parsedCacheBytes())
        } finally { dir.deleteRecursively() }
    }


    @Test fun atomicFileBackupIsRecoveredBeforeMissingOrCorruptFallback() {
        val dir = File.createTempFile("storage-poc-backup", "dir")
        check(dir.delete() && dir.mkdirs())
        try {
            val backupJson = """{"books":[{"bookUrl":"book-A","title":"A","rawJson":"{\"unknown\":1}"}],"chapters":[]}"""
            File(dir, "metadata.json.bak").writeText(backupJson)
            File(dir, "metadata.json").writeText("{broken")
            val recovered = FileJournalStore(dir)
            assertEquals(1 to 0, recovered.snapshotCounts())
            assertTrue(File(dir, "metadata.json").exists())
        } finally { dir.deleteRecursively() }
    }

    @Test fun largeTocPreservesAllIndicesAndDeterministicOrder() {
        val (dir, store) = store()
        try {
            val chapters = (0 until 1000).map { i -> FileChapterRow("url-$i", "book-large", i, "chapter-$i", "raw/$i") }
            store.seedToc(FileBookRow("book-large", "Large", "{\"unknown\":true}"), chapters)
            assertEquals(1 to 1000, store.snapshotCounts())
            assertEquals((0 until 1000).toList(), store.chapterIndices("book-large"))
        } finally { dir.deleteRecursively() }
    }

    @Test fun concurrentDistinctBooksDoNotCrossIdentity() {
        val (dir, store) = store()
        try {
            val pool = Executors.newFixedThreadPool(4)
            try {
                val futures = (0 until 12).map { i ->
                    pool.submit { store.apply(bundle("book-$i", "url-$i", 0), "parallel-$i") }
                }
                futures.forEach { it.get() }
            } finally { pool.shutdownNow() }
            assertEquals(12 to 12, store.snapshotCounts())
        } finally { dir.deleteRecursively() }
    }
}

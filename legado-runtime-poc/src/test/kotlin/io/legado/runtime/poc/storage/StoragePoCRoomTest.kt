package io.legado.runtime.poc.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StoragePoCRoomTest {
    private fun database(): StoragePoCDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StoragePoCDatabase::class.java
        ).allowMainThreadQueries().build()

    @Test fun fixtureManifestHasPinnedDeterministicHash() {
        val bytes = javaClass.getResource("/storage-poc-fixtures.json")!!.readBytes()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        assertEquals("ff52681cb45fc241825514737e753ae5fbcd73066d809b6eb6402b088fcda533", hash)
    }

    @Test fun chapterIdentityAndUniqueIndexMatchPinnedConstraints() {
        val db = database()
        try {
            val dao = db.storageDao()
            dao.applyBundle(PocBook("book-A", "A", "{\"unknown\":7}"), PocChapter("same-url", "book-A", 0, "A0", "raw/A0"))
            // Same chapter URL under a different book is a different composite primary key.
            dao.applyBundle(PocBook("book-B", "B", "{}"), PocChapter("same-url", "book-B", 0, "B0", "raw/B0"))
            assertEquals(2, dao.chapterCount())

            // Same (bookUrl,index) with another URL violates the unique index.
            try {
                dao.applyBundle(PocBook("book-A", "A", "{\"unknown\":7}"), PocChapter("other-url", "book-A", 0, "duplicate index", "raw/dup"))
                fail("expected unique (bookUrl,index) conflict")
            } catch (_: Exception) { }

            // Same (url,bookUrl) with another index violates the composite primary key.
            try {
                dao.applyBundle(PocBook("book-A", "A", "{\"unknown\":7}"), PocChapter("same-url", "book-A", 1, "duplicate URL", "raw/dup"))
                fail("expected composite primary-key conflict")
            } catch (_: Exception) { }
            assertEquals(2, dao.chapterCount())
        } finally { db.close() }
    }

    @Test fun roomTransactionRollsBackBookWhenChapterInsertConflicts() {
        val db = database()
        try {
            val dao = db.storageDao()
            dao.applyBundle(PocBook("book-A", "A", "{}"), PocChapter("url-0", "book-A", 0, "A0", "raw/A0"))
            try {
                dao.applyBundle(PocBook("book-B", "B", "{\"unknown\":true}"), PocChapter("url-1", "book-A", 0, "conflict", "raw/conflict"))
                fail("expected chapter-index conflict")
            } catch (_: Exception) { }
            assertNull("transaction must roll back the new Book row", dao.getBook("book-B"))
            assertEquals(1, dao.chapterCount())
        } finally { db.close() }
    }

    @Test fun largeTocUsesStableBookIndexOrdering() {
        val db = database()
        try {
            val chapters = (0 until 1000).map { i -> PocChapter("url-$i", "book-large", i, "chapter-$i", "raw/$i") }
            db.storageDao().insertToc(PocBook("book-large", "Large", "{\"unknown\":true}"), chapters)
            val stored = db.storageDao().getChapters("book-large")
            assertEquals(1000, stored.size)
            assertEquals(0, stored.first().chapterIndex)
            assertEquals(999, stored.last().chapterIndex)
        } finally { db.close() }
    }

    @Test fun operationReplayIsIdempotentForSameIdentityAndPayload() {
        val db = database()
        try {
            val dao = db.storageDao()
            val book = PocBook("book-A", "A", "{\"futureField\":\"kept\"}")
            val chapter = PocChapter("url-0", "book-A", 0, "A0", "raw/A0")
            dao.applyBundle(book, chapter)
            dao.applyBundle(book, chapter)
            assertEquals(1, dao.chapterCount())
            assertEquals("{\"futureField\":\"kept\"}", dao.getBook("book-A")!!.rawJson)
        } finally { db.close() }
    }
}

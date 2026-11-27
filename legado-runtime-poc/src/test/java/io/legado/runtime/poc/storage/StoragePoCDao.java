package io.legado.runtime.poc.storage;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

@Dao
public abstract class StoragePoCDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertBook(PocBook book);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertChapter(PocChapter chapter);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract void insertChapters(java.util.List<PocChapter> chapters);

    @Nullable
    @Query("SELECT * FROM poc_book WHERE book_url = :bookUrl LIMIT 1")
    public abstract PocBook getBook(String bookUrl);

    @Nullable
    @Query("SELECT * FROM poc_chapter WHERE url = :url AND book_url = :bookUrl LIMIT 1")
    public abstract PocChapter getChapter(String url, String bookUrl);

    @Nullable
    @Query("SELECT * FROM poc_chapter WHERE book_url = :bookUrl AND chapter_index = :index LIMIT 1")
    public abstract PocChapter getChapterByIndex(String bookUrl, int index);

    @Query("SELECT COUNT(*) FROM poc_chapter")
    public abstract int chapterCount();

    @Query("SELECT * FROM poc_chapter WHERE book_url = :bookUrl ORDER BY chapter_index")
    public abstract java.util.List<PocChapter> getChapters(String bookUrl);

    @Transaction
    public void insertToc(PocBook book, java.util.List<PocChapter> chapters) {
        PocBook existing = getBook(book.bookUrl);
        if (existing == null) insertBook(book);
        else if (!existing.title.equals(book.title) || !existing.rawJson.equals(book.rawJson)) {
            throw new IllegalStateException("Book fixture conflict");
        }
        insertChapters(chapters);
    }

    /** Idempotent insert with explicit conflict detection, wrapped in one Room transaction. */
    @Transaction
    public void applyBundle(PocBook book, PocChapter chapter) {
        PocBook oldBook = getBook(book.bookUrl);
        if (oldBook == null) {
            insertBook(book);
        } else if (!oldBook.title.equals(book.title) || !oldBook.rawJson.equals(book.rawJson)) {
            throw new IllegalStateException("book identity already has different payload: " + book.bookUrl);
        }

        PocChapter oldChapter = getChapter(chapter.url, chapter.bookUrl);
        if (oldChapter != null) {
            if (oldChapter.chapterIndex != chapter.chapterIndex
                    || !oldChapter.title.equals(chapter.title)
                    || !oldChapter.rawContentRef.equals(chapter.rawContentRef)) {
                throw new IllegalStateException("chapter primary-key conflict");
            }
            return;
        }
        PocChapter indexOwner = getChapterByIndex(chapter.bookUrl, chapter.chapterIndex);
        if (indexOwner != null) {
            throw new IllegalStateException("chapter index already belongs to another URL");
        }
        insertChapter(chapter);
    }
}

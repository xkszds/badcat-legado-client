package io.legado.runtime.poc.storage;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

/** Mirrors the pinned identity constraints only; it is not the upstream BookChapter class. */
@Entity(
    tableName = "poc_chapter",
    primaryKeys = {"url", "book_url"},
    indices = {@Index(value = {"book_url", "chapter_index"}, unique = true)}
)
public class PocChapter {
    @NonNull public String url = "";
    @NonNull @ColumnInfo(name = "book_url") public String bookUrl = "";
    @ColumnInfo(name = "chapter_index") public int chapterIndex;
    @NonNull public String title = "";
    @NonNull @ColumnInfo(name = "raw_content_ref") public String rawContentRef = "";

    public PocChapter() {}

    public PocChapter(@NonNull String url, @NonNull String bookUrl, int chapterIndex,
                      @NonNull String title, @NonNull String rawContentRef) {
        this.url = url;
        this.bookUrl = bookUrl;
        this.chapterIndex = chapterIndex;
        this.title = title;
        this.rawContentRef = rawContentRef;
    }
}

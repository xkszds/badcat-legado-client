package io.legado.runtime.poc.storage;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Disposable Room fixture; deliberately not a production entity or upstream replacement. */
@Entity(tableName = "poc_book")
public class PocBook {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "book_url")
    public String bookUrl = "";

    @NonNull
    public String title = "";

    /** Raw JSON payload keeps unknown upstream fields visible to the fixture. */
    @NonNull
    @ColumnInfo(name = "raw_json")
    public String rawJson = "{}";

    public PocBook() {}

    public PocBook(@NonNull String bookUrl, @NonNull String title, @NonNull String rawJson) {
        this.bookUrl = bookUrl;
        this.title = title;
        this.rawJson = rawJson;
    }
}

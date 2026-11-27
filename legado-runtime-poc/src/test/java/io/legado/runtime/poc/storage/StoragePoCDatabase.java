package io.legado.runtime.poc.storage;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {PocBook.class, PocChapter.class}, version = 1, exportSchema = false)
public abstract class StoragePoCDatabase extends RoomDatabase {
    public abstract StoragePoCDao storageDao();
}

package com.androsgreen.comicreader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schemas are exported to `app/schemas/` and committed. Once the app has shipped, every schema
 * change must bump [version] and come with a tested `Migration` (no destructive fallback).
 */
@Database(
    entities = [ComicEntity::class, CollectionEntity::class, CollectionComicEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
    abstract fun collectionDao(): CollectionDao
}

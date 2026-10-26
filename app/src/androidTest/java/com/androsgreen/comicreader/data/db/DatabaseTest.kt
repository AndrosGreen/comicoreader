package com.androsgreen.comicreader.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.androsgreen.comicreader.domain.ComicFormat
import org.junit.After
import org.junit.Before

/** Shared fixture: a fresh in-memory [AppDatabase] for every test, closed afterwards. */
abstract class DatabaseTest {

    protected lateinit var db: AppDatabase

    @Before
    fun createDatabase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() {
        db.close()
    }
}

fun comicEntity(
    title: String = "Comic",
    addedAt: Long = 0L,
    lastReadPage: Int = 0,
    lastOpenedAt: Long? = null
) = ComicEntity(
    title = title,
    fileName = "$title.cbz",
    format = ComicFormat.CBZ,
    pageCount = 20,
    coverPath = null,
    lastReadPage = lastReadPage,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt
)

fun collectionEntity(name: String, createdAt: Long = 0L) = CollectionEntity(name = name, createdAt = createdAt)

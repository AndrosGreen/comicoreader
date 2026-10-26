package com.androsgreen.comicreader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {

    /** Returns the id of the inserted comic. */
    @Insert
    suspend fun insert(comic: ComicEntity): Long

    @Query("SELECT * FROM comics WHERE id = :id")
    suspend fun getById(id: Long): ComicEntity?

    /** Newest first; `id` breaks ties between comics imported in the same millisecond. */
    @Query("SELECT * FROM comics ORDER BY addedAt DESC, id DESC")
    fun observeAll(): Flow<List<ComicEntity>>

    /** Returns the number of rows updated (0 if no comic has this id). */
    @Query("UPDATE comics SET lastReadPage = :lastReadPage, lastOpenedAt = :lastOpenedAt WHERE id = :id")
    suspend fun updateProgress(id: Long, lastReadPage: Int, lastOpenedAt: Long): Int

    /** Returns the number of rows deleted (0 if no comic has this id). */
    @Query("DELETE FROM comics WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}

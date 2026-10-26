package com.androsgreen.comicreader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    /**
     * Returns the id of the inserted collection.
     * Throws `SQLiteConstraintException` if the name is already taken.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(collection: CollectionEntity): Long

    /**
     * Returns the number of rows updated (0 if no collection has this id).
     * Throws `SQLiteConstraintException` if the name is already taken.
     */
    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String): Int

    /** Returns the number of rows deleted. Memberships cascade; the comics themselves are kept. */
    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /** Sorted by name ignoring case for display; uniqueness itself is case-sensitive. */
    @Query("SELECT * FROM collections ORDER BY name COLLATE NOCASE, name")
    fun observeAll(): Flow<List<CollectionEntity>>

    /** Like [observeAll], plus each collection's comic count and the cover of its first comic. */
    @Query(
        """
        SELECT collections.*,
            (SELECT COUNT(*) FROM collection_comics
                WHERE collection_comics.collectionId = collections.id) AS comicCount,
            (SELECT comics.coverPath FROM collection_comics
                INNER JOIN comics ON comics.id = collection_comics.comicId
                WHERE collection_comics.collectionId = collections.id
                ORDER BY collection_comics.position LIMIT 1) AS coverPath
        FROM collections
        ORDER BY collections.name COLLATE NOCASE, collections.name
        """
    )
    fun observeSummaries(): Flow<List<CollectionSummaryRow>>

    /**
     * Appends a comic at the end of a collection (position = max + 1, starting at 0).
     * Returns false if the comic was already in the collection; its position is left unchanged.
     */
    @Transaction
    suspend fun addComicAtEnd(collectionId: Long, comicId: Long): Boolean {
        val nextPosition = (maxPosition(collectionId) ?: -1) + 1
        return insertCollectionComic(CollectionComicEntity(collectionId, comicId, nextPosition)) != -1L
    }

    /** Returns the number of rows deleted (0 if the comic was not in the collection). */
    @Query("DELETE FROM collection_comics WHERE collectionId = :collectionId AND comicId = :comicId")
    suspend fun removeComic(collectionId: Long, comicId: Long): Int

    @Query(
        """
        SELECT comics.* FROM comics
        INNER JOIN collection_comics ON collection_comics.comicId = comics.id
        WHERE collection_comics.collectionId = :collectionId
        ORDER BY collection_comics.position
        """
    )
    fun observeComicsInCollection(collectionId: Long): Flow<List<ComicEntity>>

    @Query(
        """
        SELECT collections.* FROM collections
        INNER JOIN collection_comics ON collection_comics.collectionId = collections.id
        WHERE collection_comics.comicId = :comicId
        ORDER BY collections.name COLLATE NOCASE, collections.name
        """
    )
    fun observeCollectionsForComic(comicId: Long): Flow<List<CollectionEntity>>

    @Query("SELECT MAX(position) FROM collection_comics WHERE collectionId = :collectionId")
    suspend fun maxPosition(collectionId: Long): Int?

    /** Returns the row id, or -1 if the comic is already in the collection. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollectionComic(collectionComic: CollectionComicEntity): Long
}

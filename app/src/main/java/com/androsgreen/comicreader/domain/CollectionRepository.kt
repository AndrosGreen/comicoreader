package com.androsgreen.comicreader.domain

import kotlinx.coroutines.flow.Flow

interface CollectionRepository {

    /** All collections, sorted by name ignoring case. */
    fun observeCollections(): Flow<List<Collection>>

    /** The collection's comics in the order they were added. */
    fun observeComicsInCollection(collectionId: Long): Flow<List<Comic>>

    /** The collections [comicId] belongs to, sorted by name ignoring case. */
    fun observeCollectionsForComic(comicId: Long): Flow<List<Collection>>

    suspend fun createCollection(name: String): CollectionNameResult

    suspend fun renameCollection(id: Long, name: String): CollectionNameResult

    /** Removes the collection only; its comics are kept. */
    suspend fun deleteCollection(id: Long)

    /** Appends the comic at the end; does nothing if it is already in the collection. */
    suspend fun addComicToCollection(collectionId: Long, comicId: Long)

    suspend fun removeComicFromCollection(collectionId: Long, comicId: Long)
}

package com.androsgreen.comicreader.domain

import kotlinx.coroutines.flow.Flow

interface ComicRepository {

    /** All comics, newest first. */
    fun observeComics(): Flow<List<Comic>>

    suspend fun getComic(id: Long): Comic?

    /** Saves [comic] and returns its new id; `comic.id` is ignored. */
    suspend fun addComic(comic: Comic): Long

    suspend fun updateProgress(id: Long, lastReadPage: Int, lastOpenedAt: Long)

    /**
     * Removes the comic from the library and every collection, and deletes the app's stored copy
     * and cover. The original document it was imported from is never touched.
     */
    suspend fun deleteComic(id: Long)
}

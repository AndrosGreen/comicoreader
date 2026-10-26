package com.androsgreen.comicreader.domain

import kotlinx.coroutines.flow.Flow

interface ComicRepository {

    /** All comics, newest first. */
    fun observeComics(): Flow<List<Comic>>

    suspend fun getComic(id: Long): Comic?

    /** Saves [comic] and returns its new id; `comic.id` is ignored. */
    suspend fun addComic(comic: Comic): Long

    suspend fun updateProgress(id: Long, lastReadPage: Int, lastOpenedAt: Long)

    /** Also removes the comic from every collection. */
    suspend fun deleteComic(id: Long)
}

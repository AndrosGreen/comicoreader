package com.androsgreen.comicreader.data.repository

import com.androsgreen.comicreader.data.db.ComicDao
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// No explicit dispatcher: Room runs suspend and Flow queries on its own background executor.
class ComicRepositoryImpl(private val comicDao: ComicDao) : ComicRepository {

    override fun observeComics(): Flow<List<Comic>> =
        comicDao.observeAll().map { comics -> comics.map { it.toDomain() } }

    override suspend fun getComic(id: Long): Comic? = comicDao.getById(id)?.toDomain()

    override suspend fun addComic(comic: Comic): Long = comicDao.insert(comic.toEntity().copy(id = 0))

    override suspend fun updateProgress(id: Long, lastReadPage: Int, lastOpenedAt: Long) {
        comicDao.updateProgress(id, lastReadPage, lastOpenedAt)
    }

    override suspend fun deleteComic(id: Long) {
        comicDao.deleteById(id)
    }
}

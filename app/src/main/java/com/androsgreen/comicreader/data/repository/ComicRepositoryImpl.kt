package com.androsgreen.comicreader.data.repository

import com.androsgreen.comicreader.data.db.ComicDao
import com.androsgreen.comicreader.data.files.ComicFileStore
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// No explicit dispatcher: Room runs suspend and Flow queries on its own background executor.
class ComicRepositoryImpl(
    private val comicDao: ComicDao,
    private val fileStore: ComicFileStore
) : ComicRepository {

    override fun observeComics(): Flow<List<Comic>> =
        comicDao.observeAll().map { comics -> comics.map { it.toDomain() } }

    override suspend fun getComic(id: Long): Comic? = comicDao.getById(id)?.toDomain()

    override suspend fun addComic(comic: Comic): Long = comicDao.insert(comic.toEntity().copy(id = 0))

    override suspend fun updateProgress(id: Long, lastReadPage: Int, lastOpenedAt: Long) {
        comicDao.updateProgress(id, lastReadPage, lastOpenedAt)
    }

    override suspend fun deleteComic(id: Long) {
        val comic = comicDao.getById(id) ?: return
        // Row first: if deleting the files then fails, the leftover files are unreferenced and
        // harmless, whereas the reverse order could leave a library entry with no file.
        comicDao.deleteById(id)
        fileStore.deleteComicFiles(comic.fileName)
    }
}

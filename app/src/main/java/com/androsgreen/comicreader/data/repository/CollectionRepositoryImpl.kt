package com.androsgreen.comicreader.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.androsgreen.comicreader.data.db.CollectionDao
import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.CollectionNameResult
import com.androsgreen.comicreader.domain.CollectionRepository
import com.androsgreen.comicreader.domain.Comic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// No explicit dispatcher: Room runs suspend and Flow queries on its own background executor.
class CollectionRepositoryImpl(
    private val collectionDao: CollectionDao,
    private val clock: () -> Long = System::currentTimeMillis
) : CollectionRepository {

    override fun observeCollections(): Flow<List<Collection>> =
        collectionDao.observeAll().map { collections -> collections.map { it.toDomain() } }

    override fun observeComicsInCollection(collectionId: Long): Flow<List<Comic>> =
        collectionDao.observeComicsInCollection(collectionId).map { comics -> comics.map { it.toDomain() } }

    override fun observeCollectionsForComic(comicId: Long): Flow<List<Collection>> =
        collectionDao.observeCollectionsForComic(comicId).map { collections -> collections.map { it.toDomain() } }

    override suspend fun createCollection(name: String): CollectionNameResult = try {
        val id = collectionDao.insert(Collection(id = 0, name = name, createdAt = clock()).toEntity())
        CollectionNameResult.Success(id)
    } catch (e: SQLiteConstraintException) {
        CollectionNameResult.NameTaken
    }

    override suspend fun renameCollection(id: Long, name: String): CollectionNameResult = try {
        collectionDao.rename(id, name)
        CollectionNameResult.Success(id)
    } catch (e: SQLiteConstraintException) {
        CollectionNameResult.NameTaken
    }

    override suspend fun deleteCollection(id: Long) {
        collectionDao.deleteById(id)
    }

    override suspend fun addComicToCollection(collectionId: Long, comicId: Long) {
        collectionDao.addComicAtEnd(collectionId, comicId)
    }

    override suspend fun removeComicFromCollection(collectionId: Long, comicId: Long) {
        collectionDao.removeComic(collectionId, comicId)
    }
}

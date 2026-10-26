package com.androsgreen.comicreader.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androsgreen.comicreader.data.db.DatabaseTest
import com.androsgreen.comicreader.data.db.comicEntity
import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.CollectionNameResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionRepositoryImplTest : DatabaseTest() {

    private lateinit var repository: CollectionRepositoryImpl

    @Before
    fun setUp() {
        repository = CollectionRepositoryImpl(db.collectionDao(), clock = { NOW })
    }

    @Test
    fun createCollection_returnsSuccess_andStampsCreatedAtFromClock() = runTest {
        val result = repository.createCollection("Marvel")

        val id = (result as CollectionNameResult.Success).collectionId
        assertEquals(listOf(Collection(id, "Marvel", NOW)), repository.observeCollections().first())
    }

    @Test
    fun createCollection_duplicateName_returnsNameTaken() = runTest {
        repository.createCollection("Marvel")

        assertEquals(CollectionNameResult.NameTaken, repository.createCollection("Marvel"))
        assertEquals(1, repository.observeCollections().first().size)
    }

    @Test
    fun renameCollection_returnsSuccessWithSameId() = runTest {
        val id = createdId("Marvel")

        assertEquals(CollectionNameResult.Success(id), repository.renameCollection(id, "Marvel Comics"))
        assertEquals(listOf("Marvel Comics"), repository.observeCollections().first().map { it.name })
    }

    @Test
    fun renameCollection_toTakenName_returnsNameTakenAndKeepsOldName() = runTest {
        createdId("Marvel")
        val dc = createdId("DC")

        assertEquals(CollectionNameResult.NameTaken, repository.renameCollection(dc, "Marvel"))
        assertEquals(listOf("DC", "Marvel"), repository.observeCollections().first().map { it.name })
    }

    @Test
    fun addAndRemoveComic_areReflectedInBothDirections() = runTest {
        val marvel = createdId("Marvel")
        val comic = db.comicDao().insert(comicEntity(title = "Saga"))

        repository.addComicToCollection(marvel, comic)

        assertEquals(listOf("Saga"), repository.observeComicsInCollection(marvel).first().map { it.title })
        assertEquals(listOf("Marvel"), repository.observeCollectionsForComic(comic).first().map { it.name })

        repository.removeComicFromCollection(marvel, comic)

        assertEquals(emptyList<Long>(), repository.observeComicsInCollection(marvel).first().map { it.id })
    }

    @Test
    fun deleteCollection_removesIt() = runTest {
        val id = createdId("Marvel")

        repository.deleteCollection(id)

        assertEquals(emptyList<Collection>(), repository.observeCollections().first())
    }

    private suspend fun createdId(name: String): Long =
        (repository.createCollection(name) as CollectionNameResult.Success).collectionId

    private companion object {
        const val NOW = 1_234L
    }
}

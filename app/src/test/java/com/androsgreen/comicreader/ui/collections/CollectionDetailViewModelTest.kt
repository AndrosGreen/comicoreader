package com.androsgreen.comicreader.ui.collections

import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.CollectionNameResult
import com.androsgreen.comicreader.domain.CollectionRepository
import com.androsgreen.comicreader.domain.CollectionSummary
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionDetailViewModelTest {

    private val repository = FakeCollectionRepository()
    private lateinit var viewModel: CollectionDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = CollectionDetailViewModel(collectionId = 7, repository = repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsTheCollectionsNameAndComics() = runTest {
        assertTrue(state().isLoading)
        observeState()

        repository.collection.value = Collection(7, "Marvel", 0)
        repository.comics.value = listOf(comic(2), comic(1))

        assertFalse(state().isLoading)
        assertEquals("Marvel", state().name)
        assertEquals(listOf(2L, 1L), state().comics.map { it.id })
        assertEquals(listOf(7L, 7L), repository.observedIds)
    }

    @Test
    fun removeRequest_confirm_removesTheComicFromThisCollection() = runTest {
        observeState()

        viewModel.onRemoveRequest(comic(3))
        assertEquals(3L, state().pendingRemove?.id)
        viewModel.onRemoveConfirm()

        assertEquals(listOf(7L to 3L), repository.removed)
        assertNull(state().pendingRemove)
    }

    @Test
    fun removeRequest_dismiss_keepsTheComic() = runTest {
        observeState()

        viewModel.onRemoveRequest(comic(3))
        viewModel.onRemoveDismiss()

        assertNull(state().pendingRemove)
        assertEquals(emptyList<Pair<Long, Long>>(), repository.removed)
    }

    @Test
    fun deletedCollection_isReportedSoTheScreenCanClose() = runTest {
        observeState()

        repository.collection.value = null

        assertTrue(state().isDeleted)
    }

    private fun state() = viewModel.uiState.value

    /** uiState only updates while collected (WhileSubscribed), like when the screen is visible. */
    private fun TestScope.observeState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun comic(id: Long) = Comic(id, "Comic $id", "$id.cbz", ComicFormat.CBZ, 10, null, 0, id, null)

    private class FakeCollectionRepository : CollectionRepository {
        val collection = MutableStateFlow<Collection?>(Collection(7, "Marvel", 0))
        val comics = MutableStateFlow<List<Comic>>(emptyList())
        val observedIds = mutableListOf<Long>()
        val removed = mutableListOf<Pair<Long, Long>>()

        override fun observeCollection(id: Long): Flow<Collection?> {
            observedIds += id
            return collection
        }

        override fun observeComicsInCollection(collectionId: Long): Flow<List<Comic>> {
            observedIds += collectionId
            return comics
        }

        override suspend fun removeComicFromCollection(collectionId: Long, comicId: Long) {
            removed += collectionId to comicId
        }

        override fun observeCollections(): Flow<List<Collection>> = error("not used")
        override fun observeCollectionSummaries(): Flow<List<CollectionSummary>> = error("not used")
        override fun observeCollectionsForComic(comicId: Long): Flow<List<Collection>> = error("not used")
        override suspend fun createCollection(name: String): CollectionNameResult = error("not used")
        override suspend fun renameCollection(id: Long, name: String): CollectionNameResult = error("not used")
        override suspend fun deleteCollection(id: Long) = error("not used")
        override suspend fun addComicToCollection(collectionId: Long, comicId: Long) = error("not used")
    }
}

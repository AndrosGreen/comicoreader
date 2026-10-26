package com.androsgreen.comicreader.ui.library

import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.CollectionNameResult
import com.androsgreen.comicreader.domain.CollectionRepository
import com.androsgreen.comicreader.domain.CollectionSummary
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicFormat
import com.androsgreen.comicreader.domain.ComicImporter
import com.androsgreen.comicreader.domain.ComicRepository
import com.androsgreen.comicreader.domain.ImportFailure
import com.androsgreen.comicreader.domain.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
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
class LibraryViewModelTest {

    private val repository = FakeComicRepository()
    private val collections = FakeCollectionRepository()
    private val importer = FakeComicImporter()
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LibraryViewModel(repository, collections, importer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsLoading_thenShowsTheRepositorysComics() = runTest {
        repository.comics.value = null
        observeState()
        assertTrue(viewModel.uiState.value.isLoading)

        repository.comics.value = listOf(comic(2), comic(1))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(2L, 1L), state.comics.map { it.id })
    }

    @Test
    fun import_reportsProgressPerFile_thenGoesIdle() = runTest {
        observeState()

        viewModel.import(listOf("a", "b", "c"))
        assertEquals(ImportProgress(1, 3), viewModel.uiState.value.importProgress)

        importer.results.send(ImportResult.Imported(1, "A"))
        assertEquals(ImportProgress(2, 3), viewModel.uiState.value.importProgress)

        importer.results.send(ImportResult.Imported(2, "B"))
        assertEquals(ImportProgress(3, 3), viewModel.uiState.value.importProgress)

        importer.results.send(ImportResult.Imported(3, "C"))
        assertNull(viewModel.uiState.value.importProgress)
        assertEquals(listOf("a", "b", "c"), importer.sources)
    }

    @Test
    fun failures_areCollectedForTheBatch_andClearedOnceShown() = runTest {
        observeState()
        val notACbz = ImportResult.Failed("bad.cbz", ImportFailure.NOT_A_CBZ)
        val noPages = ImportResult.Failed("empty.cbz", ImportFailure.NO_PAGES)

        viewModel.import(listOf("a", "b", "c"))
        importer.results.send(notACbz)
        // Not reported until the whole batch has finished.
        assertEquals(emptyList<ImportResult.Failed>(), viewModel.uiState.value.failedImports)
        importer.results.send(ImportResult.Imported(1, "B"))
        importer.results.send(noPages)

        assertEquals(listOf(notACbz, noPages), viewModel.uiState.value.failedImports)

        viewModel.onFailuresShown()

        assertEquals(emptyList<ImportResult.Failed>(), viewModel.uiState.value.failedImports)
    }

    @Test
    fun import_whileAnotherIsRunning_isIgnored() = runTest {
        observeState()

        viewModel.import(listOf("a"))
        viewModel.import(listOf("b", "c"))
        importer.results.send(ImportResult.Imported(1, "A"))

        assertNull(viewModel.uiState.value.importProgress)
        assertEquals(listOf("a"), importer.sources)
    }

    @Test
    fun import_withNoFiles_doesNothing() = runTest {
        observeState()

        viewModel.import(emptyList())

        assertFalse(viewModel.uiState.value.isImporting)
        assertEquals(emptyList<String>(), importer.sources)
    }

    @Test
    fun deleteRequest_opensConfirmation_andDismissClosesItWithoutDeleting() = runTest {
        observeState()
        val comic = comic(1)

        viewModel.onDeleteRequest(comic)
        assertEquals(comic, viewModel.uiState.value.pendingDelete)

        viewModel.onDeleteDismiss()
        assertNull(viewModel.uiState.value.pendingDelete)
        assertEquals(emptyList<Long>(), repository.deletedIds)
    }

    @Test
    fun deleteConfirm_deletesThePendingComic_andClosesConfirmation() = runTest {
        observeState()

        viewModel.onDeleteRequest(comic(7))
        viewModel.onDeleteConfirm()

        assertEquals(listOf(7L), repository.deletedIds)
        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun deleteConfirm_withNothingPending_doesNothing() = runTest {
        observeState()

        viewModel.onDeleteConfirm()

        assertEquals(emptyList<Long>(), repository.deletedIds)
    }

    @Test
    fun longPress_opensTheActionSheet_andDismissClosesIt() = runTest {
        observeState()

        viewModel.onComicLongPress(comic(1))
        assertEquals(1L, viewModel.uiState.value.actionsFor?.id)

        viewModel.onActionsDismiss()
        assertNull(viewModel.uiState.value.actionsFor)
    }

    @Test
    fun deleteFromTheActionSheet_closesIt_andAsksForConfirmation() = runTest {
        observeState()

        viewModel.onComicLongPress(comic(1))
        viewModel.onDeleteRequest(comic(1))

        assertNull(viewModel.uiState.value.actionsFor)
        assertEquals(1L, viewModel.uiState.value.pendingDelete?.id)
    }

    @Test
    fun addToCollections_closesTheSheet_andPreChecksCurrentMemberships() = runTest {
        observeState()
        collections.all = listOf(collection(1, "A"), collection(2, "B"), collection(3, "C"))
        collections.memberships[9L] = listOf(collection(2, "B"))

        viewModel.onComicLongPress(comic(9))
        viewModel.onAddToCollectionsClick(comic(9))

        val picker = viewModel.uiState.value.collectionPicker!!
        assertNull(viewModel.uiState.value.actionsFor)
        assertEquals(listOf("A", "B", "C"), picker.collections.map { it.name })
        assertEquals(setOf(2L), picker.initialIds)
        assertEquals(setOf(2L), picker.selectedIds)
    }

    @Test
    fun save_appliesOnlyTheDifferences() = runTest {
        observeState()
        collections.all = listOf(collection(1, "A"), collection(2, "B"), collection(3, "C"))
        collections.memberships[9L] = listOf(collection(2, "B"), collection(3, "C"))

        viewModel.onAddToCollectionsClick(comic(9))
        viewModel.onCollectionToggle(1)
        viewModel.onCollectionToggle(2)
        viewModel.onCollectionPickerSave()

        assertEquals(listOf(1L to 9L), collections.added)
        assertEquals(listOf(2L to 9L), collections.removed)
        assertNull(viewModel.uiState.value.collectionPicker)
    }

    @Test
    fun togglingTwice_restoresTheSelection_andSaveChangesNothing() = runTest {
        observeState()
        collections.all = listOf(collection(1, "A"))

        viewModel.onAddToCollectionsClick(comic(9))
        viewModel.onCollectionToggle(1)
        viewModel.onCollectionToggle(1)
        viewModel.onCollectionPickerSave()

        assertEquals(emptyList<Pair<Long, Long>>(), collections.added)
        assertEquals(emptyList<Pair<Long, Long>>(), collections.removed)
    }

    @Test
    fun cancel_changesNothing() = runTest {
        observeState()
        collections.all = listOf(collection(1, "A"))

        viewModel.onAddToCollectionsClick(comic(9))
        viewModel.onCollectionToggle(1)
        viewModel.onCollectionPickerDismiss()

        assertNull(viewModel.uiState.value.collectionPicker)
        assertEquals(emptyList<Pair<Long, Long>>(), collections.added)
    }

    @Test
    fun withNoCollections_thePickerOpensEmpty() = runTest {
        observeState()

        viewModel.onAddToCollectionsClick(comic(9))

        assertEquals(emptyList<Collection>(), viewModel.uiState.value.collectionPicker?.collections)
    }

    /** uiState only updates while collected (WhileSubscribed), like when the screen is visible. */
    private fun TestScope.observeState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun collection(id: Long, name: String) = Collection(id, name, 0)

    private fun comic(id: Long) = Comic(id, "Comic $id", "$id.cbz", ComicFormat.CBZ, 10, null, 0, id, null)

    private class FakeComicRepository : ComicRepository {
        /** null = the query hasn't returned yet. */
        val comics = MutableStateFlow<List<Comic>?>(emptyList())

        override fun observeComics(): Flow<List<Comic>> = comics.filterNotNullFlow()
        override suspend fun getComic(id: Long): Comic? = error("not used")
        override suspend fun addComic(comic: Comic): Long = error("not used")
        override suspend fun updateProgress(id: Long, lastReadPage: Int, lastOpenedAt: Long) = error("not used")
        val deletedIds = mutableListOf<Long>()

        override suspend fun deleteComic(id: Long) {
            deletedIds += id
        }

        private fun MutableStateFlow<List<Comic>?>.filterNotNullFlow(): Flow<List<Comic>> =
            kotlinx.coroutines.flow.flow { collect { list -> if (list != null) emit(list) } }
    }

    private class FakeCollectionRepository : CollectionRepository {
        var all = emptyList<Collection>()
        val memberships = mutableMapOf<Long, List<Collection>>()
        val added = mutableListOf<Pair<Long, Long>>()
        val removed = mutableListOf<Pair<Long, Long>>()

        override fun observeCollections(): Flow<List<Collection>> = MutableStateFlow(all)
        override fun observeCollectionsForComic(comicId: Long): Flow<List<Collection>> =
            MutableStateFlow(memberships[comicId].orEmpty())

        override suspend fun addComicToCollection(collectionId: Long, comicId: Long) {
            added += collectionId to comicId
        }

        override suspend fun removeComicFromCollection(collectionId: Long, comicId: Long) {
            removed += collectionId to comicId
        }

        override fun observeCollection(id: Long): Flow<Collection?> = error("not used")
        override fun observeCollectionSummaries(): Flow<List<CollectionSummary>> = error("not used")
        override fun observeComicsInCollection(collectionId: Long): Flow<List<Comic>> = error("not used")
        override suspend fun createCollection(name: String): CollectionNameResult = error("not used")
        override suspend fun renameCollection(id: Long, name: String): CollectionNameResult = error("not used")
        override suspend fun deleteCollection(id: Long) = error("not used")
    }

    /** Each import suspends until the test sends its result. */
    private class FakeComicImporter : ComicImporter {
        val sources = mutableListOf<String>()
        val results = Channel<ImportResult>()

        override suspend fun import(source: String): ImportResult {
            sources += source
            return results.receive()
        }
    }
}

package com.androsgreen.comicreader.ui.library

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
    private val importer = FakeComicImporter()
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LibraryViewModel(repository, importer)
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

    /** uiState only updates while collected (WhileSubscribed), like when the screen is visible. */
    private fun TestScope.observeState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun comic(id: Long) = Comic(id, "Comic $id", "$id.cbz", ComicFormat.CBZ, 10, null, 0, id, null)

    private class FakeComicRepository : ComicRepository {
        /** null = the query hasn't returned yet. */
        val comics = MutableStateFlow<List<Comic>?>(emptyList())

        override fun observeComics(): Flow<List<Comic>> = comics.filterNotNullFlow()
        override suspend fun getComic(id: Long): Comic? = error("not used")
        override suspend fun addComic(comic: Comic): Long = error("not used")
        override suspend fun updateProgress(id: Long, lastReadPage: Int, lastOpenedAt: Long) = error("not used")
        override suspend fun deleteComic(id: Long) = error("not used")

        private fun MutableStateFlow<List<Comic>?>.filterNotNullFlow(): Flow<List<Comic>> =
            kotlinx.coroutines.flow.flow { collect { list -> if (list != null) emit(list) } }
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

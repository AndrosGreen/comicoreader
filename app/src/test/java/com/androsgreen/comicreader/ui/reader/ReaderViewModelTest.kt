package com.androsgreen.comicreader.ui.reader

import androidx.lifecycle.ViewModelStore
import com.androsgreen.comicreader.data.archive.ComicArchive
import com.androsgreen.comicreader.data.archive.ComicOpener
import com.androsgreen.comicreader.data.archive.InvalidArchiveException
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicFormat
import com.androsgreen.comicreader.domain.ComicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val saga = Comic(7, "Saga #1", "saga.cbz", ComicFormat.CBZ, 3, null, 0, 0, null)
    private val repository = FakeComicRepository()
    private val archive = FakeArchive(listOf("p1.jpg", "p2.jpg", "p3.jpg"))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTheTitleAndOnePagePerArchiveEntry() {
        repository.comics[7] = saga

        val state = viewModel(opener = { archive }).uiState.value

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Saga #1", state.title)
        assertEquals(listOf(0, 1, 2), state.pages.map { it.index })
        assertTrue(state.pages.all { it.archive === archive && it.fileName == "saga.cbz" })
    }

    @Test
    fun unknownComic_isNotFound() {
        val state = viewModel(opener = { error("must not open") }).uiState.value

        assertEquals(ReaderError.NOT_FOUND, state.error)
    }

    @Test
    fun unreadableFile_isUnreadable() {
        repository.comics[7] = saga

        val state = viewModel(opener = { throw InvalidArchiveException("corrupt") }).uiState.value

        assertEquals(ReaderError.UNREADABLE, state.error)
    }

    @Test
    fun archiveWithNoPages_isUnreadable_andIsClosed() {
        repository.comics[7] = saga
        val empty = FakeArchive(emptyList())
        val store = ViewModelStore()

        val viewModel = viewModel(opener = { empty })
        store.put("reader", viewModel)
        assertEquals(ReaderError.UNREADABLE, viewModel.uiState.value.error)

        store.clear()
        assertTrue(empty.closed)
    }

    @Test
    fun theArchiveStaysOpenWhileReading_andClosesWithTheViewModel() {
        repository.comics[7] = saga
        val store = ViewModelStore()

        store.put("reader", viewModel(opener = { archive }))
        assertFalse(archive.closed)

        store.clear()
        assertTrue(archive.closed)
    }

    @Test
    fun startsOnTheFirstPage_withTheOverlayHidden() {
        repository.comics[7] = saga

        val state = viewModel(opener = { archive }).uiState.value

        assertEquals(0, state.currentPage)
        assertFalse(state.isOverlayVisible)
    }

    @Test
    fun pageChanges_areTracked_andOutOfRangeIndicesIgnored() {
        repository.comics[7] = saga
        val viewModel = viewModel(opener = { archive })

        viewModel.onPageChanged(2)
        assertEquals(2, viewModel.uiState.value.currentPage)

        viewModel.onPageChanged(3)
        viewModel.onPageChanged(-1)
        assertEquals(2, viewModel.uiState.value.currentPage)
    }

    @Test
    fun toggleOverlay_showsThenHidesIt() {
        repository.comics[7] = saga
        val viewModel = viewModel(opener = { archive })

        viewModel.onToggleOverlay()
        assertTrue(viewModel.uiState.value.isOverlayVisible)

        viewModel.onToggleOverlay()
        assertFalse(viewModel.uiState.value.isOverlayVisible)
    }

    @Test
    fun opensAtTheSavedPage_andRecordsTheOpenTime() {
        repository.comics[7] = saga.copy(lastReadPage = 2)

        val state = viewModel(opener = { archive }).uiState.value

        assertEquals(2, state.currentPage)
        assertEquals(listOf(Progress(7, page = 2, at = NOW)), repository.progress)
    }

    @Test
    fun aSavedPageBeyondTheEnd_isClampedToTheLastPage() {
        repository.comics[7] = saga.copy(lastReadPage = 10)

        assertEquals(2, viewModel(opener = { archive }).uiState.value.currentPage)
    }

    @Test
    fun pageChanges_areSaved_onceEach() {
        repository.comics[7] = saga
        val viewModel = viewModel(opener = { archive })

        viewModel.onPageChanged(1)
        viewModel.onPageChanged(1)
        viewModel.onPageChanged(2)

        assertEquals(listOf(0, 1, 2), repository.progress.map { it.page })
    }

    @Test
    fun outOfRangePages_areNotSaved() {
        repository.comics[7] = saga
        val viewModel = viewModel(opener = { archive })

        viewModel.onPageChanged(5)

        assertEquals(listOf(0), repository.progress.map { it.page })
    }

    @Test
    fun nothingIsSaved_whenTheComicCannotBeOpened() {
        repository.comics[7] = saga

        viewModel(opener = { throw InvalidArchiveException("corrupt") })

        assertEquals(emptyList<Progress>(), repository.progress)
    }

    private fun viewModel(opener: ComicOpener) =
        ReaderViewModel(comicId = 7, comicRepository = repository, comicOpener = opener, clock = { NOW })

    private data class Progress(val comicId: Long, val page: Int, val at: Long)

    private class FakeArchive(override val pageNames: List<String>) : ComicArchive {
        var closed = false

        override fun openPage(index: Int): InputStream = error("not used")

        override fun close() {
            closed = true
        }
    }

    private companion object {
        const val NOW = 1_234L
    }

    private class FakeComicRepository : ComicRepository {
        val comics = mutableMapOf<Long, Comic>()
        val progress = mutableListOf<Progress>()

        override suspend fun getComic(id: Long): Comic? = comics[id]

        override suspend fun updateProgress(id: Long, lastReadPage: Int, lastOpenedAt: Long) {
            progress += Progress(id, lastReadPage, lastOpenedAt)
        }

        override fun observeComics(): Flow<List<Comic>> = error("not used")
        override suspend fun addComic(comic: Comic): Long = error("not used")
        override suspend fun deleteComic(id: Long) = error("not used")
    }
}

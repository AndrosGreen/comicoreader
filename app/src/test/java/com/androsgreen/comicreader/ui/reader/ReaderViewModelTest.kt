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

    private fun viewModel(opener: ComicOpener) =
        ReaderViewModel(comicId = 7, comicRepository = repository, comicOpener = opener)

    private class FakeArchive(override val pageNames: List<String>) : ComicArchive {
        var closed = false

        override fun openPage(index: Int): InputStream = error("not used")

        override fun close() {
            closed = true
        }
    }

    private class FakeComicRepository : ComicRepository {
        val comics = mutableMapOf<Long, Comic>()

        override suspend fun getComic(id: Long): Comic? = comics[id]

        override fun observeComics(): Flow<List<Comic>> = error("not used")
        override suspend fun addComic(comic: Comic): Long = error("not used")
        override suspend fun updateProgress(id: Long, lastReadPage: Int, lastOpenedAt: Long) = error("not used")
        override suspend fun deleteComic(id: Long) = error("not used")
    }
}

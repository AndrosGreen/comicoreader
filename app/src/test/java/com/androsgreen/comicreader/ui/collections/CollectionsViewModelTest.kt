package com.androsgreen.comicreader.ui.collections

import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.CollectionNameResult
import com.androsgreen.comicreader.domain.CollectionRepository
import com.androsgreen.comicreader.domain.CollectionSummary
import com.androsgreen.comicreader.domain.Comic
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModelTest {

    private val marvel = Collection(1, "Marvel", 0)
    private val repository = FakeCollectionRepository()
    private lateinit var viewModel: CollectionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = CollectionsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsTheRepositorysSummaries() = runTest {
        observeState()
        val summaries = listOf(CollectionSummary(marvel, 3, "/covers/a.webp"))

        repository.summaries.value = summaries

        assertFalse(state().isLoading)
        assertEquals(summaries, state().collections)
    }

    @Test
    fun create_withAValidName_trimsIt_andClosesTheDialog() = runTest {
        observeState()

        viewModel.onCreateClick()
        assertEquals(NameDialogState(NameDialogMode.Create, name = ""), state().nameDialog)
        viewModel.onNameChange("  Reading list  ")
        viewModel.onNameConfirm()

        assertEquals(listOf("Reading list"), repository.created)
        assertNull(state().nameDialog)
    }

    @Test
    fun create_withABlankName_showsAnError_withoutCallingTheRepository() = runTest {
        observeState()

        viewModel.onCreateClick()
        viewModel.onNameChange("   ")
        viewModel.onNameConfirm()

        assertEquals(NameError.BLANK, state().nameDialog?.error)
        assertEquals(emptyList<String>(), repository.created)
    }

    @Test
    fun create_withATakenName_showsAnError_andKeepsTheDialogOpen() = runTest {
        observeState()
        repository.takenNames += "Marvel"

        viewModel.onCreateClick()
        viewModel.onNameChange("Marvel")
        viewModel.onNameConfirm()

        assertEquals(NameDialogState(NameDialogMode.Create, name = "Marvel", error = NameError.TAKEN), state().nameDialog)
    }

    @Test
    fun editingTheName_clearsThePreviousError() = runTest {
        observeState()

        viewModel.onCreateClick()
        viewModel.onNameConfirm()
        viewModel.onNameChange("M")

        assertNull(state().nameDialog?.error)
    }

    @Test
    fun rename_prefillsTheName_andRenamesThatCollection() = runTest {
        observeState()

        viewModel.onRenameClick(marvel)
        assertEquals(NameDialogState(NameDialogMode.Rename(marvel), name = "Marvel"), state().nameDialog)
        viewModel.onNameChange("Marvel Comics")
        viewModel.onNameConfirm()

        assertEquals(listOf(1L to "Marvel Comics"), repository.renamed)
        assertNull(state().nameDialog)
    }

    @Test
    fun rename_toATakenName_showsAnError() = runTest {
        observeState()
        repository.takenNames += "DC"

        viewModel.onRenameClick(marvel)
        viewModel.onNameChange("DC")
        viewModel.onNameConfirm()

        assertEquals(NameError.TAKEN, state().nameDialog?.error)
    }

    @Test
    fun dismissingTheNameDialog_closesItWithoutSaving() = runTest {
        observeState()

        viewModel.onCreateClick()
        viewModel.onNameChange("Marvel")
        viewModel.onNameDismiss()

        assertNull(state().nameDialog)
        assertEquals(emptyList<String>(), repository.created)
    }

    @Test
    fun delete_confirm_deletesThePendingCollection() = runTest {
        observeState()

        viewModel.onDeleteClick(marvel)
        assertEquals(marvel, state().pendingDelete)
        viewModel.onDeleteConfirm()

        assertEquals(listOf(1L), repository.deleted)
        assertNull(state().pendingDelete)
    }

    @Test
    fun delete_dismiss_keepsTheCollection() = runTest {
        observeState()

        viewModel.onDeleteClick(marvel)
        viewModel.onDeleteDismiss()

        assertNull(state().pendingDelete)
        assertEquals(emptyList<Long>(), repository.deleted)
    }

    private fun state() = viewModel.uiState.value

    /** uiState only updates while collected (WhileSubscribed), like when the screen is visible. */
    private fun TestScope.observeState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private class FakeCollectionRepository : CollectionRepository {
        val summaries = MutableStateFlow<List<CollectionSummary>>(emptyList())
        val takenNames = mutableSetOf<String>()
        val created = mutableListOf<String>()
        val renamed = mutableListOf<Pair<Long, String>>()
        val deleted = mutableListOf<Long>()

        override fun observeCollectionSummaries(): Flow<List<CollectionSummary>> = summaries

        override suspend fun createCollection(name: String): CollectionNameResult {
            if (name in takenNames) return CollectionNameResult.NameTaken
            created += name
            return CollectionNameResult.Success(created.size.toLong())
        }

        override suspend fun renameCollection(id: Long, name: String): CollectionNameResult {
            if (name in takenNames) return CollectionNameResult.NameTaken
            renamed += id to name
            return CollectionNameResult.Success(id)
        }

        override suspend fun deleteCollection(id: Long) {
            deleted += id
        }

        override fun observeCollections(): Flow<List<Collection>> = error("not used")
        override fun observeComicsInCollection(collectionId: Long): Flow<List<Comic>> = error("not used")
        override fun observeCollectionsForComic(comicId: Long): Flow<List<Collection>> = error("not used")
        override suspend fun addComicToCollection(collectionId: Long, comicId: Long) = error("not used")
        override suspend fun removeComicFromCollection(collectionId: Long, comicId: Long) = error("not used")
    }
}

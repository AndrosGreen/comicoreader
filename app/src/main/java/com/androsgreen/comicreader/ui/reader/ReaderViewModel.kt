package com.androsgreen.comicreader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androsgreen.comicreader.data.archive.ComicOpener
import com.androsgreen.comicreader.domain.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Opens [comicId]'s archive once and keeps it open while the reader is on screen; it is closed
 * when the ViewModel is cleared. [comicId] comes from the navigation route as a Koin parameter.
 */
class ReaderViewModel(
    private val comicId: Long,
    private val comicRepository: ComicRepository,
    private val comicOpener: ComicOpener
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val comic = comicRepository.getComic(comicId)
            ?: return fail(ReaderError.NOT_FOUND)
        val archive = try {
            comicOpener.open(comic)
        } catch (e: IOException) {
            return fail(ReaderError.UNREADABLE)
        }
        // Closed with the ViewModel, after any page loads still using it are cancelled.
        addCloseable(archive)
        if (archive.pageNames.isEmpty()) return fail(ReaderError.UNREADABLE)
        _uiState.value = ReaderUiState(
            isLoading = false,
            title = comic.title,
            pages = archive.pageNames.indices.map { ComicPage(archive, comic.fileName, it) }
        )
    }

    fun onPageChanged(index: Int) {
        _uiState.update { state ->
            if (index in state.pages.indices) state.copy(currentPage = index) else state
        }
    }

    fun onToggleOverlay() {
        _uiState.update { it.copy(isOverlayVisible = !it.isOverlayVisible) }
    }

    private fun fail(error: ReaderError) {
        _uiState.value = ReaderUiState(isLoading = false, error = error)
    }
}

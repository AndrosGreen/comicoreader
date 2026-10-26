package com.androsgreen.comicreader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androsgreen.comicreader.data.archive.ComicOpener
import com.androsgreen.comicreader.domain.ComicRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Opens [comicId]'s archive once and keeps it open while the reader is on screen; it is closed
 * when the ViewModel is cleared. [comicId] comes from the navigation route as a Koin parameter.
 *
 * Reading progress: the comic opens at its saved page, and every page change is saved together
 * with the time (so opening also updates "last opened").
 */
class ReaderViewModel(
    private val comicId: Long,
    private val comicRepository: ComicRepository,
    private val comicOpener: ComicOpener,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    /** The page to persist. A StateFlow conflates rapid page turns, so only the latest is written. */
    private val pageToSave = MutableStateFlow<Int?>(null)

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
        // The saved page may be out of range if the stored file ever changed; clamp it.
        val startPage = comic.lastReadPage.coerceIn(0, archive.pageNames.lastIndex)
        _uiState.value = ReaderUiState(
            isLoading = false,
            title = comic.title,
            pages = archive.pageNames.indices.map { ComicPage(archive, comic.fileName, it) },
            currentPage = startPage
        )
        pageToSave.value = startPage
        viewModelScope.launch { saveProgress() }
    }

    /** Writes pages one at a time, in order; a write already started finishes even if the reader closes. */
    private suspend fun saveProgress() {
        pageToSave.filterNotNull().collect { page ->
            withContext(NonCancellable) { comicRepository.updateProgress(comicId, page, clock()) }
        }
    }

    fun onPageChanged(index: Int) {
        if (index !in _uiState.value.pages.indices) return
        _uiState.update { it.copy(currentPage = index) }
        pageToSave.value = index
    }

    fun onToggleOverlay() {
        _uiState.update { it.copy(isOverlayVisible = !it.isOverlayVisible) }
    }

    private fun fail(error: ReaderError) {
        _uiState.value = ReaderUiState(isLoading = false, error = error)
    }
}

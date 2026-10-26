package com.androsgreen.comicreader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicImporter
import com.androsgreen.comicreader.domain.ComicRepository
import com.androsgreen.comicreader.domain.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val comicRepository: ComicRepository,
    private val importer: ComicImporter
) : ViewModel() {

    private data class ImportState(
        val progress: ImportProgress? = null,
        val failed: List<ImportResult.Failed> = emptyList()
    )

    private val importState = MutableStateFlow(ImportState())
    private val pendingDelete = MutableStateFlow<Comic?>(null)

    val uiState: StateFlow<LibraryUiState> =
        combine(comicRepository.observeComics(), importState, pendingDelete) { comics, import, toDelete ->
            LibraryUiState(
                comics = comics,
                isLoading = false,
                importProgress = import.progress,
                failedImports = import.failed,
                pendingDelete = toDelete
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    /** Imports [sources] (document Uris) one at a time. Ignored while an import is running. */
    fun import(sources: List<String>) {
        if (sources.isEmpty() || importState.value.progress != null) return
        importState.update { it.copy(progress = ImportProgress(1, sources.size)) }
        viewModelScope.launch {
            val failures = mutableListOf<ImportResult.Failed>()
            try {
                sources.forEachIndexed { index, source ->
                    importState.update { it.copy(progress = ImportProgress(index + 1, sources.size)) }
                    val result = importer.import(source)
                    if (result is ImportResult.Failed) failures += result
                }
            } finally {
                importState.update { it.copy(progress = null, failed = it.failed + failures) }
            }
        }
    }

    fun onFailuresShown() {
        importState.update { it.copy(failed = emptyList()) }
    }

    /** Asks for confirmation before deleting [comic]. */
    fun onDeleteRequest(comic: Comic) {
        pendingDelete.value = comic
    }

    fun onDeleteDismiss() {
        pendingDelete.value = null
    }

    fun onDeleteConfirm() {
        val comic = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch { comicRepository.deleteComic(comic.id) }
    }
}

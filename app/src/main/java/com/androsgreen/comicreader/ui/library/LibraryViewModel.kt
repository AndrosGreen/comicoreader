package com.androsgreen.comicreader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    comicRepository: ComicRepository,
    private val importer: ComicImporter
) : ViewModel() {

    private data class ImportState(
        val progress: ImportProgress? = null,
        val failed: List<ImportResult.Failed> = emptyList()
    )

    private val importState = MutableStateFlow(ImportState())

    val uiState: StateFlow<LibraryUiState> =
        combine(comicRepository.observeComics(), importState) { comics, import ->
            LibraryUiState(
                comics = comics,
                isLoading = false,
                importProgress = import.progress,
                failedImports = import.failed
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
}

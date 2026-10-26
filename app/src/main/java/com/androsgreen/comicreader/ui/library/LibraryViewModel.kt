package com.androsgreen.comicreader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androsgreen.comicreader.domain.CollectionRepository
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicImporter
import com.androsgreen.comicreader.domain.ComicRepository
import com.androsgreen.comicreader.domain.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val comicRepository: ComicRepository,
    private val collectionRepository: CollectionRepository,
    private val importer: ComicImporter
) : ViewModel() {

    private data class ImportState(
        val progress: ImportProgress? = null,
        val failed: List<ImportResult.Failed> = emptyList()
    )

    private data class Dialogs(
        val actionsFor: Comic? = null,
        val pendingDelete: Comic? = null,
        val collectionPicker: CollectionPickerState? = null
    )

    private val importState = MutableStateFlow(ImportState())
    private val dialogs = MutableStateFlow(Dialogs())

    val uiState: StateFlow<LibraryUiState> =
        combine(comicRepository.observeComics(), importState, dialogs) { comics, import, dialogs ->
            LibraryUiState(
                comics = comics,
                isLoading = false,
                importProgress = import.progress,
                failedImports = import.failed,
                pendingDelete = dialogs.pendingDelete,
                actionsFor = dialogs.actionsFor,
                collectionPicker = dialogs.collectionPicker
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

    /** Opens the action sheet (Add to collections, Delete) for [comic]. */
    fun onComicLongPress(comic: Comic) {
        dialogs.update { it.copy(actionsFor = comic) }
    }

    fun onActionsDismiss() {
        dialogs.update { it.copy(actionsFor = null) }
    }

    /** Closes the action sheet and asks for confirmation before deleting [comic]. */
    fun onDeleteRequest(comic: Comic) {
        dialogs.update { it.copy(actionsFor = null, pendingDelete = comic) }
    }

    fun onDeleteDismiss() {
        dialogs.update { it.copy(pendingDelete = null) }
    }

    fun onDeleteConfirm() {
        val comic = dialogs.value.pendingDelete ?: return
        dialogs.update { it.copy(pendingDelete = null) }
        viewModelScope.launch { comicRepository.deleteComic(comic.id) }
    }

    /** Closes the action sheet and opens the collection picker with [comic]'s current memberships checked. */
    fun onAddToCollectionsClick(comic: Comic) {
        dialogs.update { it.copy(actionsFor = null) }
        viewModelScope.launch {
            val collections = collectionRepository.observeCollections().first()
            val memberships = collectionRepository.observeCollectionsForComic(comic.id).first().map { it.id }.toSet()
            dialogs.update {
                it.copy(collectionPicker = CollectionPickerState(comic, collections, memberships, memberships))
            }
        }
    }

    fun onCollectionToggle(collectionId: Long) {
        dialogs.update { dialogs ->
            val picker = dialogs.collectionPicker ?: return@update dialogs
            val selected = if (collectionId in picker.selectedIds) {
                picker.selectedIds - collectionId
            } else {
                picker.selectedIds + collectionId
            }
            dialogs.copy(collectionPicker = picker.copy(selectedIds = selected))
        }
    }

    fun onCollectionPickerDismiss() {
        dialogs.update { it.copy(collectionPicker = null) }
    }

    /** Adds the comic to newly checked collections and removes it from unchecked ones; nothing else changes. */
    fun onCollectionPickerSave() {
        val picker = dialogs.value.collectionPicker ?: return
        dialogs.update { it.copy(collectionPicker = null) }
        viewModelScope.launch {
            (picker.selectedIds - picker.initialIds).forEach { collectionRepository.addComicToCollection(it, picker.comic.id) }
            (picker.initialIds - picker.selectedIds).forEach { collectionRepository.removeComicFromCollection(it, picker.comic.id) }
        }
    }
}

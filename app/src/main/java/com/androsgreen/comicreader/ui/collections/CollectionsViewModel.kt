package com.androsgreen.comicreader.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.CollectionNameResult
import com.androsgreen.comicreader.domain.CollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollectionsViewModel(private val repository: CollectionRepository) : ViewModel() {

    private data class Dialogs(
        val name: NameDialogState? = null,
        val delete: Collection? = null
    )

    private val dialogs = MutableStateFlow(Dialogs())

    val uiState: StateFlow<CollectionsUiState> =
        combine(repository.observeCollectionSummaries(), dialogs) { collections, dialogs ->
            CollectionsUiState(
                collections = collections,
                isLoading = false,
                nameDialog = dialogs.name,
                pendingDelete = dialogs.delete
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionsUiState())

    fun onCreateClick() {
        dialogs.update { it.copy(name = NameDialogState(NameDialogMode.Create, name = "")) }
    }

    fun onRenameClick(collection: Collection) {
        dialogs.update { it.copy(name = NameDialogState(NameDialogMode.Rename(collection), name = collection.name)) }
    }

    /** Editing the name clears any previous error. */
    fun onNameChange(name: String) {
        dialogs.update { it.copy(name = it.name?.copy(name = name, error = null)) }
    }

    fun onNameDismiss() {
        dialogs.update { it.copy(name = null) }
    }

    /** Trims the name; a blank or taken name keeps the dialog open with an error. */
    fun onNameConfirm() {
        val dialog = dialogs.value.name ?: return
        val name = dialog.name.trim()
        if (name.isEmpty()) {
            dialogs.update { it.copy(name = it.name?.copy(error = NameError.BLANK)) }
            return
        }
        viewModelScope.launch {
            val result = when (val mode = dialog.mode) {
                NameDialogMode.Create -> repository.createCollection(name)
                is NameDialogMode.Rename -> repository.renameCollection(mode.collection.id, name)
            }
            when (result) {
                is CollectionNameResult.Success -> dialogs.update { it.copy(name = null) }
                CollectionNameResult.NameTaken -> dialogs.update { it.copy(name = it.name?.copy(error = NameError.TAKEN)) }
            }
        }
    }

    fun onDeleteClick(collection: Collection) {
        dialogs.update { it.copy(delete = collection) }
    }

    fun onDeleteDismiss() {
        dialogs.update { it.copy(delete = null) }
    }

    fun onDeleteConfirm() {
        val collection = dialogs.value.delete ?: return
        dialogs.update { it.copy(delete = null) }
        viewModelScope.launch { repository.deleteCollection(collection.id) }
    }
}

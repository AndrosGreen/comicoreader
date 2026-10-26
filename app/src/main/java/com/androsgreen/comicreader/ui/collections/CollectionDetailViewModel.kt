package com.androsgreen.comicreader.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androsgreen.comicreader.domain.CollectionRepository
import com.androsgreen.comicreader.domain.Comic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [collectionId] comes from the navigation route, passed in as a Koin parameter. */
class CollectionDetailViewModel(
    private val collectionId: Long,
    private val repository: CollectionRepository
) : ViewModel() {

    private val pendingRemove = MutableStateFlow<Comic?>(null)

    val uiState: StateFlow<CollectionDetailUiState> =
        combine(
            repository.observeCollection(collectionId),
            repository.observeComicsInCollection(collectionId),
            pendingRemove
        ) { collection, comics, toRemove ->
            if (collection == null) {
                CollectionDetailUiState(isLoading = false, isDeleted = true)
            } else {
                CollectionDetailUiState(
                    name = collection.name,
                    comics = comics,
                    isLoading = false,
                    pendingRemove = toRemove
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionDetailUiState())

    /** Asks for confirmation before removing [comic] from this collection. */
    fun onRemoveRequest(comic: Comic) {
        pendingRemove.value = comic
    }

    fun onRemoveDismiss() {
        pendingRemove.value = null
    }

    fun onRemoveConfirm() {
        val comic = pendingRemove.value ?: return
        pendingRemove.value = null
        viewModelScope.launch { repository.removeComicFromCollection(collectionId, comic.id) }
    }
}

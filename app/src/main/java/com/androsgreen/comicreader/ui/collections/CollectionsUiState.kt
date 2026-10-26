package com.androsgreen.comicreader.ui.collections

import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.CollectionSummary

data class CollectionsUiState(
    /** Sorted by name, ignoring case. */
    val collections: List<CollectionSummary> = emptyList(),
    /** True until the first list has loaded. */
    val isLoading: Boolean = true,
    /** The create/rename dialog, if open. */
    val nameDialog: NameDialogState? = null,
    /** The collection awaiting delete confirmation, if any. */
    val pendingDelete: Collection? = null
)

data class NameDialogState(
    val mode: NameDialogMode,
    val name: String,
    val error: NameError? = null
)

sealed interface NameDialogMode {
    data object Create : NameDialogMode
    data class Rename(val collection: Collection) : NameDialogMode
}

enum class NameError { BLANK, TAKEN }

package com.androsgreen.comicreader.ui.library

import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ImportResult

data class LibraryUiState(
    /** Newest first. */
    val comics: List<Comic> = emptyList(),
    /** True until the first list of comics has loaded. */
    val isLoading: Boolean = true,
    /** Non-null while an import is running. */
    val importProgress: ImportProgress? = null,
    /** Failures from finished imports, kept until the screen reports them shown. */
    val failedImports: List<ImportResult.Failed> = emptyList(),
    /** The comic awaiting delete confirmation, if any. */
    val pendingDelete: Comic? = null,
    /** The comic whose long-press action sheet is open, if any. */
    val actionsFor: Comic? = null,
    /** The "Add to collections" dialog, if open. */
    val collectionPicker: CollectionPickerState? = null
) {
    val isImporting: Boolean get() = importProgress != null
}

/**
 * Which collections [comic] should belong to. [initialIds] are its memberships when the dialog
 * opened; saving applies only the difference between them and [selectedIds].
 */
data class CollectionPickerState(
    val comic: Comic,
    /** All collections, sorted by name; empty if none exist yet. */
    val collections: List<Collection>,
    val initialIds: Set<Long>,
    val selectedIds: Set<Long>
)

/** Importing file number [current] (1-based) of [total]. */
data class ImportProgress(val current: Int, val total: Int)

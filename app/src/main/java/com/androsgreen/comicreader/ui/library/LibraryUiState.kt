package com.androsgreen.comicreader.ui.library

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
    val pendingDelete: Comic? = null
) {
    val isImporting: Boolean get() = importProgress != null
}

/** Importing file number [current] (1-based) of [total]. */
data class ImportProgress(val current: Int, val total: Int)

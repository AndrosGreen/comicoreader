package com.androsgreen.comicreader.domain

sealed interface ImportResult {
    data class Imported(val comicId: Long, val title: String) : ImportResult

    /** [name] is the picked file's display name, for showing which file failed. */
    data class Failed(val name: String, val reason: ImportFailure) : ImportResult
}

enum class ImportFailure {
    CANNOT_READ_FILE,
    NOT_A_CBZ,
    NO_PAGES,
    FIRST_PAGE_UNREADABLE
}

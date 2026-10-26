package com.androsgreen.comicreader.ui.reader

data class ReaderUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    /** One entry per page, in reading order; empty until loaded. */
    val pages: List<ComicPage> = emptyList(),
    /** 0-based index of the page on screen. */
    val currentPage: Int = 0,
    /** The overlay (back, title, page X of Y) and the system bars are shown together. */
    val isOverlayVisible: Boolean = false,
    val error: ReaderError? = null
)

enum class ReaderError {
    /** The comic is no longer in the library. */
    NOT_FOUND,

    /** The comic's stored file is missing, unreadable, or has no pages. */
    UNREADABLE
}

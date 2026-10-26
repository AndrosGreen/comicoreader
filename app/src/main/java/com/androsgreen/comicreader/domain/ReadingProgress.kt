package com.androsgreen.comicreader.domain

/** How far a comic has been read, for display. */
sealed interface ReadingProgress {
    /** Never opened. */
    data object Unread : ReadingProgress

    /** Opened; [page] is 1-based, and [fraction] counts the current page as read. */
    data class InProgress(val page: Int, val pageCount: Int) : ReadingProgress {
        val fraction: Float get() = page.toFloat() / pageCount
    }

    /** The last page has been reached. */
    data object Finished : ReadingProgress
}

val Comic.readingProgress: ReadingProgress
    get() = when {
        lastOpenedAt == null -> ReadingProgress.Unread
        lastReadPage >= pageCount - 1 -> ReadingProgress.Finished
        else -> ReadingProgress.InProgress(page = lastReadPage + 1, pageCount = pageCount)
    }

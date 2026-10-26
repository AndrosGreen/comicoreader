package com.androsgreen.comicreader.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingProgressTest {

    private val comic = Comic(1, "Saga", "saga.cbz", ComicFormat.CBZ, pageCount = 30, coverPath = null, addedAt = 0, lastOpenedAt = null)

    @Test
    fun neverOpened_isUnread() {
        assertEquals(ReadingProgress.Unread, comic.readingProgress)
    }

    @Test
    fun openedOnTheFirstPage_isInProgress_withThatPageCounted() {
        val progress = comic.copy(lastOpenedAt = 5).readingProgress

        assertEquals(ReadingProgress.InProgress(page = 1, pageCount = 30), progress)
        assertEquals(1f / 30, (progress as ReadingProgress.InProgress).fraction)
    }

    @Test
    fun midway_isInProgress() {
        assertEquals(
            ReadingProgress.InProgress(page = 7, pageCount = 30),
            comic.copy(lastReadPage = 6, lastOpenedAt = 5).readingProgress
        )
    }

    @Test
    fun lastPageReached_isFinished() {
        assertEquals(ReadingProgress.Finished, comic.copy(lastReadPage = 29, lastOpenedAt = 5).readingProgress)
    }

    @Test
    fun aOnePageComic_isFinishedOnceOpened() {
        assertEquals(ReadingProgress.Finished, comic.copy(pageCount = 1, lastOpenedAt = 5).readingProgress)
    }
}

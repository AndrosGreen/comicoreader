package com.androsgreen.comicreader.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androsgreen.comicreader.data.db.DatabaseTest
import com.androsgreen.comicreader.data.db.comicEntity
import com.androsgreen.comicreader.domain.Comic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicRepositoryImplTest : DatabaseTest() {

    private lateinit var repository: ComicRepositoryImpl

    @Before
    fun setUp() {
        repository = ComicRepositoryImpl(db.comicDao())
    }

    @Test
    fun addComic_ignoresGivenId_andGetComicReturnsIt() = runTest {
        val comic = comic(title = "Saga").copy(id = 99L)

        val id = repository.addComic(comic)

        assertEquals(comic.copy(id = id), repository.getComic(id))
    }

    @Test
    fun getComic_unknownId_returnsNull() = runTest {
        assertNull(repository.getComic(42L))
    }

    @Test
    fun observeComics_returnsDomainModelsNewestFirst() = runTest {
        val old = repository.addComic(comic(title = "Old", addedAt = 100L))
        val new = repository.addComic(comic(title = "New", addedAt = 200L))

        assertEquals(listOf(new, old), repository.observeComics().first().map { it.id })
    }

    @Test
    fun updateProgress_isVisibleOnTheComic() = runTest {
        val id = repository.addComic(comic())

        repository.updateProgress(id, lastReadPage = 5, lastOpenedAt = 700L)

        val updated = repository.getComic(id)!!
        assertEquals(5, updated.lastReadPage)
        assertEquals(700L, updated.lastOpenedAt)
    }

    @Test
    fun deleteComic_removesIt() = runTest {
        val id = repository.addComic(comic())

        repository.deleteComic(id)

        assertNull(repository.getComic(id))
    }

    private fun comic(title: String = "Comic", addedAt: Long = 0L): Comic =
        comicEntity(title = title, addedAt = addedAt).toDomain()
}

package com.androsgreen.comicreader.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicDaoTest : DatabaseTest() {

    private lateinit var dao: ComicDao

    @Before
    fun setUp() {
        dao = db.comicDao()
    }

    @Test
    fun insert_thenGetById_returnsComic() = runTest {
        val comic = comicEntity(title = "Saga", addedAt = 100L)

        val id = dao.insert(comic)

        assertEquals(comic.copy(id = id), dao.getById(id))
    }

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        assertNull(dao.getById(42L))
    }

    @Test
    fun observeAll_returnsNewestFirst() = runTest {
        val oldId = dao.insert(comicEntity(title = "Old", addedAt = 100L))
        val newId = dao.insert(comicEntity(title = "New", addedAt = 300L))
        val middleId = dao.insert(comicEntity(title = "Middle", addedAt = 200L))

        assertEquals(listOf(newId, middleId, oldId), dao.observeAll().first().map { it.id })
    }

    @Test
    fun observeAll_sameAddedAt_newestIdFirst() = runTest {
        val firstId = dao.insert(comicEntity(title = "First", addedAt = 100L))
        val secondId = dao.insert(comicEntity(title = "Second", addedAt = 100L))

        assertEquals(listOf(secondId, firstId), dao.observeAll().first().map { it.id })
    }

    @Test
    fun updateProgress_updatesPageAndLastOpenedAt() = runTest {
        val id = dao.insert(comicEntity(addedAt = 100L))

        val updated = dao.updateProgress(id, lastReadPage = 7, lastOpenedAt = 500L)

        assertEquals(1, updated)
        val comic = dao.getById(id)!!
        assertEquals(7, comic.lastReadPage)
        assertEquals(500L, comic.lastOpenedAt)
    }

    @Test
    fun updateProgress_unknownId_returnsZero() = runTest {
        assertEquals(0, dao.updateProgress(42L, lastReadPage = 1, lastOpenedAt = 500L))
    }

    @Test
    fun deleteById_removesComic() = runTest {
        val id = dao.insert(comicEntity())

        assertEquals(1, dao.deleteById(id))
        assertNull(dao.getById(id))
        assertEquals(0, dao.deleteById(id))
    }

    @Test
    fun observeAll_reEmitsAfterChanges() = runTest {
        val emissions = Channel<List<ComicEntity>>(Channel.UNLIMITED)
        val collector = launch { dao.observeAll().collect { emissions.send(it) } }

        // Wait for the initial emission before changing the table, so each change maps to one emission.
        assertEquals(emptyList<ComicEntity>(), emissions.receive())

        val id = dao.insert(comicEntity(title = "Saga"))
        assertEquals(listOf(id), emissions.receive().map { it.id })

        dao.updateProgress(id, lastReadPage = 3, lastOpenedAt = 500L)
        assertEquals(3, emissions.receive().single().lastReadPage)

        dao.deleteById(id)
        assertEquals(emptyList<ComicEntity>(), emissions.receive())

        collector.cancel()
    }
}

package com.androsgreen.comicreader.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionDaoTest : DatabaseTest() {

    private lateinit var dao: CollectionDao
    private lateinit var comicDao: ComicDao

    @Before
    fun setUp() {
        dao = db.collectionDao()
        comicDao = db.comicDao()
    }

    // Spec delete rules

    @Test
    fun deletingComic_removesItFromAllCollections() = runTest {
        val marvel = dao.insert(collectionEntity("Marvel"))
        val favourites = dao.insert(collectionEntity("Favourites"))
        val deleted = comicDao.insert(comicEntity(title = "Deleted"))
        val kept = comicDao.insert(comicEntity(title = "Kept"))
        dao.addComicAtEnd(marvel, deleted)
        dao.addComicAtEnd(marvel, kept)
        dao.addComicAtEnd(favourites, deleted)

        comicDao.deleteById(deleted)

        assertEquals(listOf(kept), comicIdsIn(marvel))
        assertEquals(emptyList<Long>(), comicIdsIn(favourites))
        assertEquals(emptyList<CollectionEntity>(), dao.observeCollectionsForComic(deleted).first())
    }

    @Test
    fun deletingCollection_keepsItsComics() = runTest {
        val marvel = dao.insert(collectionEntity("Marvel"))
        val favourites = dao.insert(collectionEntity("Favourites"))
        val comic = comicDao.insert(comicEntity())
        dao.addComicAtEnd(marvel, comic)
        dao.addComicAtEnd(favourites, comic)

        assertEquals(1, dao.deleteById(marvel))

        assertNotNull(comicDao.getById(comic))
        assertEquals(listOf(favourites), dao.observeCollectionsForComic(comic).first().map { it.id })
        assertEquals(emptyList<Long>(), comicIdsIn(marvel))
    }

    // Names

    @Test
    fun insert_duplicateName_throws() = runTest {
        dao.insert(collectionEntity("Batman"))

        val error = runCatching { dao.insert(collectionEntity("Batman")) }.exceptionOrNull()

        assertTrue("expected SQLiteConstraintException, got $error", error is SQLiteConstraintException)
        assertEquals(listOf("Batman"), dao.observeAll().first().map { it.name })
    }

    @Test
    fun insert_nameDifferingOnlyInCase_isAllowed() = runTest {
        dao.insert(collectionEntity("Batman"))
        dao.insert(collectionEntity("batman"))

        assertEquals(2, dao.observeAll().first().size)
    }

    @Test
    fun rename_updatesName() = runTest {
        val id = dao.insert(collectionEntity("Marvel"))

        assertEquals(1, dao.rename(id, "Marvel Comics"))

        assertEquals(listOf("Marvel Comics"), dao.observeAll().first().map { it.name })
    }

    @Test
    fun rename_unknownId_returnsZero() = runTest {
        assertEquals(0, dao.rename(42L, "Anything"))
    }

    @Test
    fun rename_toExistingName_throwsAndKeepsOldName() = runTest {
        dao.insert(collectionEntity("Marvel"))
        val dc = dao.insert(collectionEntity("DC"))

        val error = runCatching { dao.rename(dc, "Marvel") }.exceptionOrNull()

        assertTrue("expected SQLiteConstraintException, got $error", error is SQLiteConstraintException)
        assertEquals(listOf("DC", "Marvel"), dao.observeAll().first().map { it.name })
    }

    @Test
    fun observeAll_sortsByNameIgnoringCase() = runTest {
        dao.insert(collectionEntity("Zatanna"))
        dao.insert(collectionEntity("batman"))
        dao.insert(collectionEntity("Avengers"))

        assertEquals(listOf("Avengers", "batman", "Zatanna"), dao.observeAll().first().map { it.name })
    }

    // Membership

    @Test
    fun addComicAtEnd_listsComicsInInsertionOrder() = runTest {
        val collection = dao.insert(collectionEntity("Marvel"))
        val first = comicDao.insert(comicEntity(title = "First", addedAt = 300L))
        val second = comicDao.insert(comicEntity(title = "Second", addedAt = 100L))
        val third = comicDao.insert(comicEntity(title = "Third", addedAt = 200L))

        // Added in a different order from both id and addedAt, so only `position` explains the result.
        assertTrue(dao.addComicAtEnd(collection, third))
        assertTrue(dao.addComicAtEnd(collection, first))
        assertTrue(dao.addComicAtEnd(collection, second))

        assertEquals(listOf(third, first, second), comicIdsIn(collection))
    }

    @Test
    fun addComicAtEnd_alreadyInCollection_returnsFalseAndKeepsPosition() = runTest {
        val collection = dao.insert(collectionEntity("Marvel"))
        val first = comicDao.insert(comicEntity(title = "First"))
        val second = comicDao.insert(comicEntity(title = "Second"))
        dao.addComicAtEnd(collection, first)
        dao.addComicAtEnd(collection, second)

        assertFalse(dao.addComicAtEnd(collection, first))

        assertEquals(listOf(first, second), comicIdsIn(collection))
    }

    @Test
    fun addComicAtEnd_afterRemove_stillAppendsAtEnd() = runTest {
        val collection = dao.insert(collectionEntity("Marvel"))
        val a = comicDao.insert(comicEntity(title = "A"))
        val b = comicDao.insert(comicEntity(title = "B"))
        val c = comicDao.insert(comicEntity(title = "C"))
        dao.addComicAtEnd(collection, a)
        dao.addComicAtEnd(collection, b)
        dao.removeComic(collection, a)

        dao.addComicAtEnd(collection, c)

        assertEquals(listOf(b, c), comicIdsIn(collection))
    }

    @Test
    fun removeComic_removesOnlyFromThatCollection() = runTest {
        val marvel = dao.insert(collectionEntity("Marvel"))
        val favourites = dao.insert(collectionEntity("Favourites"))
        val comic = comicDao.insert(comicEntity())
        dao.addComicAtEnd(marvel, comic)
        dao.addComicAtEnd(favourites, comic)

        assertEquals(1, dao.removeComic(marvel, comic))

        assertEquals(emptyList<Long>(), comicIdsIn(marvel))
        assertEquals(listOf(comic), comicIdsIn(favourites))
        assertNotNull(comicDao.getById(comic))
        assertEquals(0, dao.removeComic(marvel, comic))
    }

    @Test
    fun observeCollectionsForComic_returnsOnlyItsCollectionsSortedByName() = runTest {
        val zatanna = dao.insert(collectionEntity("Zatanna"))
        val batman = dao.insert(collectionEntity("batman"))
        val other = dao.insert(collectionEntity("Other"))
        val comic = comicDao.insert(comicEntity(title = "Comic"))
        val otherComic = comicDao.insert(comicEntity(title = "Other comic"))
        dao.addComicAtEnd(zatanna, comic)
        dao.addComicAtEnd(batman, comic)
        dao.addComicAtEnd(other, otherComic)

        assertEquals(listOf(batman, zatanna), dao.observeCollectionsForComic(comic).first().map { it.id })
    }

    @Test
    fun observeCollectionsForComic_notInAnyCollection_isEmpty() = runTest {
        val comic = comicDao.insert(comicEntity())

        assertEquals(emptyList<CollectionEntity>(), dao.observeCollectionsForComic(comic).first())
    }

    @Test
    fun observeById_emitsTheCollection_thenNullOnceDeleted() = runTest {
        val id = dao.insert(collectionEntity("Marvel"))

        assertEquals("Marvel", dao.observeById(id).first()?.name)
        dao.deleteById(id)
        assertNull(dao.observeById(id).first())
    }

    // Summaries

    @Test
    fun observeSummaries_countsComics_andTakesTheCoverOfTheFirstComicByPosition() = runTest {
        val marvel = dao.insert(collectionEntity("Marvel"))
        dao.insert(collectionEntity("Empty"))
        val first = comicDao.insert(comicEntity(title = "First").copy(coverPath = "/covers/first.webp"))
        val second = comicDao.insert(comicEntity(title = "Second").copy(coverPath = "/covers/second.webp"))
        // Added second-then-first, so the cover must follow position, not id or insertion of the comic.
        dao.addComicAtEnd(marvel, second)
        dao.addComicAtEnd(marvel, first)

        val summaries = dao.observeSummaries().first().associateBy { it.collection.name }

        assertEquals(2, summaries.getValue("Marvel").comicCount)
        assertEquals("/covers/second.webp", summaries.getValue("Marvel").coverPath)
        assertEquals(0, summaries.getValue("Empty").comicCount)
        assertNull(summaries.getValue("Empty").coverPath)
    }

    @Test
    fun observeSummaries_followsMembershipChanges() = runTest {
        val marvel = dao.insert(collectionEntity("Marvel"))
        val first = comicDao.insert(comicEntity(title = "First").copy(coverPath = "/covers/first.webp"))
        val second = comicDao.insert(comicEntity(title = "Second").copy(coverPath = "/covers/second.webp"))
        dao.addComicAtEnd(marvel, first)
        dao.addComicAtEnd(marvel, second)

        dao.removeComic(marvel, first)
        val afterRemove = dao.observeSummaries().first().single()
        comicDao.deleteById(second)
        val afterDelete = dao.observeSummaries().first().single()

        assertEquals(1 to "/covers/second.webp", afterRemove.comicCount to afterRemove.coverPath)
        assertEquals(0 to null, afterDelete.comicCount to afterDelete.coverPath)
    }

    @Test
    fun observeSummaries_sortsByNameIgnoringCase() = runTest {
        dao.insert(collectionEntity("Zatanna"))
        dao.insert(collectionEntity("batman"))
        dao.insert(collectionEntity("Avengers"))

        assertEquals(listOf("Avengers", "batman", "Zatanna"), dao.observeSummaries().first().map { it.collection.name })
    }

    private suspend fun comicIdsIn(collectionId: Long): List<Long> =
        dao.observeComicsInCollection(collectionId).first().map { it.id }
}

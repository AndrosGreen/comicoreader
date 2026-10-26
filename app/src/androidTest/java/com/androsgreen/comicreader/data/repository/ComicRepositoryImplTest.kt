package com.androsgreen.comicreader.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androsgreen.comicreader.data.db.DatabaseTest
import com.androsgreen.comicreader.data.db.comicEntity
import com.androsgreen.comicreader.data.files.ComicFileStore
import com.androsgreen.comicreader.domain.Comic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ComicRepositoryImplTest : DatabaseTest() {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: ComicFileStore
    private lateinit var repository: ComicRepositoryImpl

    @Before
    fun setUp() {
        store = ComicFileStore(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            tempFolder.newFolder("root")
        )
        repository = ComicRepositoryImpl(db.comicDao(), store)
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
    fun deleteComic_removesTheRowAndTheStoredFiles() = runTest {
        val id = repository.addComic(comic(fileName = "a.cbz"))
        val (file, cover) = storedFiles("a.cbz")

        repository.deleteComic(id)

        assertNull(repository.getComic(id))
        assertFalse(file.exists())
        assertFalse(cover.exists())
    }

    @Test
    fun deleteComic_whenFilesAreAlreadyGone_stillRemovesTheRow() = runTest {
        val id = repository.addComic(comic(fileName = "a.cbz"))

        repository.deleteComic(id)

        assertNull(repository.getComic(id))
    }

    @Test
    fun deleteComic_unknownId_leavesOtherComicsAlone() = runTest {
        val id = repository.addComic(comic(fileName = "a.cbz"))
        val (file, cover) = storedFiles("a.cbz")

        repository.deleteComic(id + 1)

        assertEquals(id, repository.getComic(id)?.id)
        assertTrue(file.exists())
        assertTrue(cover.exists())
    }

    private fun comic(title: String = "Comic", addedAt: Long = 0L, fileName: String = "$title.cbz"): Comic =
        comicEntity(title = title, addedAt = addedAt).toDomain().copy(fileName = fileName)

    /** Creates the comic file and cover the store would hold for [fileName]. */
    private fun storedFiles(fileName: String): Pair<File, File> {
        val file = store.comicFile(fileName).apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1)) }
        val cover = store.coverFile(fileName).apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(2)) }
        return file to cover
    }
}

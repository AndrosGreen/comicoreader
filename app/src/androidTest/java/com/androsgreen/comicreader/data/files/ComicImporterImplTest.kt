package com.androsgreen.comicreader.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androsgreen.comicreader.data.archive.CbzReader
import com.androsgreen.comicreader.data.db.DatabaseTest
import com.androsgreen.comicreader.data.repository.ComicRepositoryImpl
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicFormat
import com.androsgreen.comicreader.domain.ComicRepository
import com.androsgreen.comicreader.domain.ImportFailure
import com.androsgreen.comicreader.domain.ImportResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ComicImporterImplTest : DatabaseTest() {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var root: File
    private lateinit var sources: File
    private lateinit var store: ComicFileStore
    private lateinit var repository: ComicRepositoryImpl
    private lateinit var importer: ComicImporterImpl

    @Before
    fun setUp() {
        root = tempFolder.newFolder("root")
        sources = tempFolder.newFolder("sources")
        store = ComicFileStore(ApplicationProvider.getApplicationContext<Context>().contentResolver, root)
        repository = ComicRepositoryImpl(db.comicDao(), store)
        importer = ComicImporterImpl(store, CbzReader(), CoverGenerator(), repository, clock = { NOW })
    }

    @Test
    fun validCbz_isSavedWithItsFileAndCover() = runTest {
        // p2 is not an image: the cover only succeeds if it comes from the first page in reading order.
        val source = cbz("Saga #1.cbz", "p2.jpg" to NOT_AN_IMAGE, "p1.jpg" to jpeg(), "ComicInfo.xml" to NOT_AN_IMAGE)

        val result = importer.import(source)

        val id = (result as ImportResult.Imported).comicId
        assertEquals("Saga #1", result.title)
        val comic = repository.getComic(id)!!
        val expected = Comic(
            id = id,
            title = "Saga #1",
            fileName = comic.fileName,
            format = ComicFormat.CBZ,
            pageCount = 2,
            coverPath = store.coverFile(comic.fileName).path,
            lastReadPage = 0,
            addedAt = NOW,
            lastOpenedAt = null
        )
        assertEquals(expected, comic)
        assertTrue(store.comicFile(comic.fileName).isFile)
        assertTrue(BitmapFactory.decodeFile(comic.coverPath) != null)
    }

    @Test
    fun corruptFile_failsAsNotACbz_andLeavesNothing() = runTest {
        val file = File(sources, "bad.cbz").apply { writeText("not a zip") }

        assertEquals(ImportResult.Failed("bad.cbz", ImportFailure.NOT_A_CBZ), importer.import(uriOf(file)))
        assertNothingLeft()
    }

    @Test
    fun zipWithoutImages_failsAsNoPages_andLeavesNothing() = runTest {
        val source = cbz("empty.cbz", "ComicInfo.xml" to NOT_AN_IMAGE)

        assertEquals(ImportResult.Failed("empty.cbz", ImportFailure.NO_PAGES), importer.import(source))
        assertNothingLeft()
    }

    @Test
    fun undecodableFirstPage_failsAsFirstPageUnreadable_andLeavesNothing() = runTest {
        val source = cbz("broken.cbz", "p1.jpg" to NOT_AN_IMAGE, "p2.jpg" to jpeg())

        assertEquals(ImportResult.Failed("broken.cbz", ImportFailure.FIRST_PAGE_UNREADABLE), importer.import(source))
        assertNothingLeft()
    }

    @Test
    fun missingFile_failsAsCannotReadFile_andLeavesNothing() = runTest {
        val result = importer.import(uriOf(File(sources, "missing.cbz")))

        assertEquals(ImportResult.Failed("missing.cbz", ImportFailure.CANNOT_READ_FILE), result)
        assertNothingLeft()
    }

    @Test
    fun unexpectedErrorWhileSaving_failsAsUnexpected_andLeavesNothing() = runTest {
        val failingRepository = object : ComicRepository by repository {
            override suspend fun addComic(comic: Comic): Long = throw IllegalStateException("disk I/O error")
        }
        val importer = ComicImporterImpl(store, CbzReader(), CoverGenerator(), failingRepository, clock = { NOW })

        val result = importer.import(cbz("Saga.cbz", "p1.jpg" to jpeg()))

        assertEquals(ImportResult.Failed("Saga.cbz", ImportFailure.UNEXPECTED), result)
        assertNothingLeft()
    }

    @Test
    fun oneFailureInABatch_doesNotAffectTheOthers() = runTest {
        val results = listOf(
            cbz("A.cbz", "p1.jpg" to jpeg()),
            cbz("B.cbz", "notes.txt" to NOT_AN_IMAGE),
            cbz("C.cbz", "p1.jpg" to jpeg())
        ).map { importer.import(it) }

        assertEquals(listOf("A", "C"), results.filterIsInstance<ImportResult.Imported>().map { it.title })
        assertEquals(ImportResult.Failed("B.cbz", ImportFailure.NO_PAGES), results[1])
        assertEquals(2, repository.observeComics().first().size)
        assertEquals(2, filesIn("comics").size)
        assertEquals(2, filesIn("covers").size)
    }

    private suspend fun assertNothingLeft() {
        assertEquals(emptyList<File>(), filesIn("comics"))
        assertEquals(emptyList<File>(), filesIn("covers"))
        assertEquals(emptyList<Comic>(), repository.observeComics().first())
    }

    private fun filesIn(dir: String): List<File> = File(root, dir).listFiles()?.toList().orEmpty()

    private fun cbz(name: String, vararg entries: Pair<String, ByteArray>): String {
        val file = File(sources, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((entryName, bytes) in entries) {
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return uriOf(file)
    }

    private fun uriOf(file: File): String = Uri.fromFile(file).toString()

    private fun jpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(100, 150, Bitmap.Config.RGB_565).apply { eraseColor(Color.CYAN) }
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private companion object {
        const val NOW = 1_234L
        val NOT_AN_IMAGE = "not an image".toByteArray()
    }
}

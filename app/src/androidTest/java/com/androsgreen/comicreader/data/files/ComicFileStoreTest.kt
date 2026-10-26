package com.androsgreen.comicreader.data.files

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ComicFileStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sourceDir = File(context.cacheDir, "file-store-test")

    // Unique per run, so MediaStore never renames a test file because of a leftover duplicate.
    private val runId = UUID.randomUUID().toString().take(8)
    private val insertedUris = mutableListOf<Uri>()

    private lateinit var root: File
    private lateinit var store: ComicFileStore

    @Before
    fun setUp() {
        sourceDir.mkdirs()
        root = tempFolder.newFolder("root")
        store = ComicFileStore(context.contentResolver, root)
    }

    @After
    fun tearDown() {
        sourceDir.deleteRecursively()
        insertedUris.forEach { context.contentResolver.delete(it, null, null) }
    }

    @Test
    fun copyIn_downloadsUri_copiesBytesUnderAUuidName() = runTest {
        val bytes = ByteArray(100_000) { it.toByte() }

        val fileName = store.copyIn(downloadsUri("Saga #1 $runId.cbz", bytes))

        assertTrue(fileName, fileName.matches(Regex("[0-9a-f-]{36}\\.cbz")))
        val stored = File(root, "comics/$fileName")
        assertEquals(stored, store.comicFile(fileName))
        assertArrayEquals(bytes, stored.readBytes())
    }

    @Test
    fun copyIn_sameDocumentTwice_givesDistinctFiles() = runTest {
        val uri = downloadsUri("Saga $runId.cbz", byteArrayOf(1, 2, 3))

        assertNotEquals(store.copyIn(uri), store.copyIn(uri))
        assertEquals(2, comicsDirFiles().size)
    }

    @Test
    fun copyIn_streamFailingHalfway_leavesNoFile() = runTest {
        val error = runCatching { store.copyIn(FailingInputStream(bytesBeforeFailure = 50_000)) }.exceptionOrNull()

        assertTrue("expected IOException, got $error", error is IOException)
        assertEquals(emptyList<File>(), comicsDirFiles())
    }

    @Test
    fun copyIn_missingDocument_throwsAndCreatesNothing() = runTest {
        val missing = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, Long.MAX_VALUE)

        val error = runCatching { store.copyIn(missing) }.exceptionOrNull()

        assertTrue("expected FileNotFoundException, got $error", error is FileNotFoundException)
        assertEquals(emptyList<File>(), comicsDirFiles())
    }

    @Test
    fun displayName_downloadsUri_returnsProviderName() = runTest {
        val name = "Saga #1 $runId.cbz"

        assertEquals(name, store.displayName(downloadsUri(name, byteArrayOf(0))))
    }

    @Test
    fun displayName_withoutProvider_fallsBackToLastPathSegment() = runTest {
        val file = File(sourceDir, "Paper Girls.cbz").apply { writeBytes(byteArrayOf(0)) }

        assertEquals("Paper Girls.cbz", store.displayName(Uri.fromFile(file)))
    }

    @Test
    fun coverFile_sharesTheComicFilesName() {
        assertEquals(File(root, "covers/3f2a.webp"), store.coverFile("3f2a.cbz"))
    }

    @Test
    fun deleteComicFiles_removesComicAndCover() = runTest {
        val fileName = store.copyIn(downloadsUri("Saga $runId.cbz", byteArrayOf(1)))
        val cover = store.coverFile(fileName).apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(2)) }

        store.deleteComicFiles(fileName)

        assertFalse(store.comicFile(fileName).exists())
        assertFalse(cover.exists())
    }

    @Test
    fun deleteComicFiles_missingFiles_doesNotThrow() = runTest {
        store.deleteComicFiles("never-stored.cbz")
    }

    /** Adds a file to the shared Downloads collection, like a comic the user downloaded. */
    private fun downloadsUri(name: String, bytes: ByteArray): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
        insertedUris += uri
        context.contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }
        return uri
    }

    private fun comicsDirFiles(): List<File> = File(root, "comics").listFiles()?.toList().orEmpty()

    /** Returns [bytesBeforeFailure] zero bytes, then fails like a dropped connection or full disk. */
    private class FailingInputStream(private val bytesBeforeFailure: Int) : InputStream() {
        private var position = 0

        override fun read(): Int {
            if (position >= bytesBeforeFailure) throw IOException("Simulated read failure")
            position++
            return 0
        }
    }
}

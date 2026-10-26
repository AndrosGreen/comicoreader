package com.androsgreen.comicreader.ui.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Precision
import com.androsgreen.comicreader.data.archive.CbzReader
import com.androsgreen.comicreader.data.archive.ComicArchive
import kotlinx.coroutines.test.runTest
import org.junit.After
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
class ComicPageFetcherTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var archive: ComicArchive

    @Before
    fun setUp() {
        // p2 is not an image; p1 is a large page, so downsampling is observable.
        archive = CbzReader().open(cbz("p1.jpg" to jpeg(2000, 3000), "p2.jpg" to "not an image".toByteArray()))
    }

    @After
    fun tearDown() {
        archive.close()
    }

    @Test
    fun page_loadsThroughTheAppImageLoader_downsampledToTheRequestedSize() = runTest {
        val result = load(SingletonImageLoader.get(context), ComicPage(archive, "saga.cbz", index = 0))

        result as SuccessResult
        assertEquals(400, result.image.width)
        assertEquals(600, result.image.height)
        assertEquals("comic-page:saga.cbz:0", result.memoryCacheKey?.key)
    }

    @Test
    fun pageThatIsNotAnImage_failsCleanly() = runTest {
        val result = load(SingletonImageLoader.get(context), ComicPage(archive, "saga.cbz", index = 1))

        assertTrue("expected ErrorResult, got $result", result is ErrorResult)
    }

    private suspend fun load(imageLoader: ImageLoader, page: ComicPage) = imageLoader.execute(
        ImageRequest.Builder(context)
            .data(page)
            .size(400, 600)
            .precision(Precision.EXACT)
            .build()
    )

    private fun cbz(vararg entries: Pair<String, ByteArray>): File {
        val file = tempFolder.newFile("comic.cbz")
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    private fun jpeg(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply { eraseColor(Color.YELLOW) }
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }
}

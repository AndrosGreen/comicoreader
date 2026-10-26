package com.androsgreen.comicreader.data.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class CoverGeneratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val generator = CoverGenerator()

    @Test
    fun typicalPage_becomesA600x900WebP() = runTest {
        val cover = coverFor(page(1000, 1500))

        val header = cover.readBytes()
        assertEquals("RIFF", String(header, 0, 4, Charsets.US_ASCII))
        assertEquals("WEBP", String(header, 8, 4, Charsets.US_ASCII))
        assertEquals(600 to 900, sizeOf(cover))
    }

    @Test
    fun veryLargePage_isScaledDownToTheBox() = runTest {
        assertEquals(600 to 900, sizeOf(coverFor(page(3000, 4500))))
    }

    @Test
    fun landscapeSpread_isLimitedByWidth() = runTest {
        assertEquals(600 to 400, sizeOf(coverFor(page(3000, 2000))))
    }

    @Test
    fun tallStrip_isLimitedByHeight() = runTest {
        assertEquals(90 to 900, sizeOf(coverFor(page(800, 8000))))
    }

    @Test
    fun smallPage_isNotUpscaled() = runTest {
        assertEquals(200 to 300, sizeOf(coverFor(page(200, 300))))
    }

    @Test
    fun existingCover_isReplaced() = runTest {
        val target = File(tempFolder.root, "covers/1.webp")
        generator.generate(page(1000, 1500).inputStream(), target)

        generator.generate(page(200, 300).inputStream(), target)

        assertEquals(200 to 300, sizeOf(target))
    }

    @Test
    fun undecodablePage_throwsAndLeavesNoFiles() = runTest {
        val target = File(tempFolder.root, "covers/1.webp")

        val error = runCatching {
            generator.generate("not an image".toByteArray().inputStream(), target)
        }.exceptionOrNull()

        assertTrue("expected IOException, got $error", error is IOException)
        assertFalse(target.exists())
        assertFalse(File(target.path + ".tmp").exists())
    }

    private suspend fun coverFor(pageBytes: ByteArray): File {
        val target = File(tempFolder.root, "covers/1.webp")
        generator.generate(pageBytes.inputStream(), target)
        return target
    }

    /** A solid-colour JPEG page; RGB_565 keeps the large test bitmaps cheap to create. */
    private fun page(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply { eraseColor(Color.MAGENTA) }
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun sizeOf(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        return options.outWidth to options.outHeight
    }
}

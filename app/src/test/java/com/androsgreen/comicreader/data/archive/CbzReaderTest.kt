package com.androsgreen.comicreader.data.archive

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CbzReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val reader = CbzReader()

    @Test
    fun pagesAreNaturallySorted() {
        val file = cbz("page10.jpg", "page2.jpg", "page1.jpg")

        assertEquals(listOf("page1.jpg", "page2.jpg", "page10.jpg"), pageNames(file))
    }

    @Test
    fun pagesInFoldersAreSortedByFullPath() {
        val file = cbz("ch10/p1.jpg", "ch2/p10.jpg", "ch2/p2.jpg", "cover.jpg")

        assertEquals(listOf("ch2/p2.jpg", "ch2/p10.jpg", "ch10/p1.jpg", "cover.jpg"), pageNames(file))
    }

    @Test
    fun allImageExtensionsAreAccepted_inAnyCase() {
        val file = cbz("a.jpg", "b.JPEG", "c.png", "d.WebP", "e.avif", "f.GIF")

        assertEquals(6, pageNames(file).size)
    }

    @Test
    fun nonImageFilesAreIgnored() {
        val file = cbz("ComicInfo.xml", "readme.txt", "noextension", "p1.jpg")

        assertEquals(listOf("p1.jpg"), pageNames(file))
    }

    @Test
    fun directoriesAreIgnored_evenWithAnImageLikeName() {
        val file = cbz("folder.jpg/", "folder.jpg/p1.jpg")

        assertEquals(listOf("folder.jpg/p1.jpg"), pageNames(file))
    }

    @Test
    fun hiddenFilesAndFoldersAreIgnored() {
        val file = cbz(".cover.jpg", ".hidden/p1.jpg", "ch1/.thumb.jpg", "ch1/p1.jpg")

        assertEquals(listOf("ch1/p1.jpg"), pageNames(file))
    }

    @Test
    fun macOsMetadataIsIgnored() {
        val file = cbz("__MACOSX/p1.jpg", "__MACOSX/ch1/._p1.jpg", "comic/__MACOSX/p2.jpg", "p1.jpg")

        assertEquals(listOf("p1.jpg"), pageNames(file))
    }

    @Test
    fun windowsSeparatorsAreUnderstood() {
        val file = cbz("__MACOSX\\p1.jpg", "ch1\\p1.jpg")

        assertEquals(listOf("ch1\\p1.jpg"), pageNames(file))
    }

    @Test
    fun openPage_returnsThatPagesBytes() {
        val file = cbz(
            "p2.jpg" to byteArrayOf(2, 2),
            "p1.jpg" to byteArrayOf(1),
            "p10.jpg" to byteArrayOf(10, 10, 10)
        )

        reader.open(file).use { archive ->
            assertArrayEquals(byteArrayOf(1), archive.openPage(0).use { it.readBytes() })
            assertArrayEquals(byteArrayOf(10, 10, 10), archive.openPage(2).use { it.readBytes() })
        }
    }

    @Test
    fun archiveWithNoImages_opensWithNoPages() {
        val file = cbz("ComicInfo.xml", "notes/readme.txt")

        assertEquals(emptyList<String>(), pageNames(file))
    }

    @Test(expected = InvalidArchiveException::class)
    fun corruptFile_throwsInvalidArchiveException() {
        val file = tempFolder.newFile("corrupt.cbz").apply { writeText("this is not a zip file") }

        reader.open(file)
    }

    @Test(expected = InvalidArchiveException::class)
    fun emptyFile_throwsInvalidArchiveException() {
        reader.open(tempFolder.newFile("empty.cbz"))
    }

    private fun pageNames(file: File): List<String> = reader.open(file).use { it.pageNames }

    /** Names ending in `/` become directory entries; every file gets a single placeholder byte. */
    private fun cbz(vararg names: String): File =
        cbz(*names.map { it to byteArrayOf(0) }.toTypedArray())

    private fun cbz(vararg entries: Pair<String, ByteArray>): File {
        val file = tempFolder.newFile()
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                if (!name.endsWith("/")) zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }
}

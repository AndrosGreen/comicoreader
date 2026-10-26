package com.androsgreen.comicreader.data.archive

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class CbzReader : ComicArchiveReader {

    override fun open(file: File): ComicArchive {
        val zip = try {
            ZipFile(file)
        } catch (e: IOException) {
            throw InvalidArchiveException("Not a readable CBZ file: ${file.name}", e)
        }
        val pages = try {
            zip.entries().asSequence()
                .filter { !it.isDirectory && isPagePath(it.name) }
                .sortedWith(compareBy(NaturalOrderComparator) { it.name })
                .toList()
        } catch (e: Exception) {
            zip.close()
            throw InvalidArchiveException("Could not read the contents of ${file.name}", e)
        }
        return CbzArchive(zip, pages)
    }

    private class CbzArchive(
        private val zip: ZipFile,
        private val pages: List<ZipEntry>
    ) : ComicArchive {

        override val pageNames: List<String> = pages.map { it.name }

        override fun openPage(index: Int): InputStream = zip.getInputStream(pages[index])

        override fun close() = zip.close()
    }

    internal companion object {
        private val PAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "avif", "gif")

        /** True for image files that are not hidden and not macOS resource-fork metadata. */
        fun isPagePath(path: String): Boolean {
            // Some archivers write Windows separators, so accept both.
            val segments = path.split('/', '\\').filter { it.isNotEmpty() }
            if (segments.isEmpty()) return false
            if (segments.any { it == "__MACOSX" || it.startsWith(".") }) return false
            return segments.last().substringAfterLast('.', "").lowercase() in PAGE_EXTENSIONS
        }
    }
}

package com.androsgreen.comicreader.data.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.UUID

/**
 * App-private storage for imported comics and their covers:
 * `<rootDir>/comics/<uuid>.cbz` and `<rootDir>/covers/<uuid>.webp`. A cover shares its comic's
 * file name, so it can be created before the comic has a database id.
 */
class ComicFileStore(
    private val contentResolver: ContentResolver,
    rootDir: File
) {
    private val comicsDir = File(rootDir, "comics")
    private val coversDir = File(rootDir, "covers")

    /**
     * Copies the document at [uri] into storage and returns its new file name.
     * On any failure, including cancellation, no partial file is left behind.
     */
    suspend fun copyIn(uri: Uri): String {
        val input = withContext(Dispatchers.IO) { contentResolver.openInputStream(uri) }
            ?: throw FileNotFoundException("Cannot open $uri")
        return input.use { copyIn(it) }
    }

    /** Copies [input] into storage and returns the new file name. Does not close [input]. */
    suspend fun copyIn(input: InputStream): String {
        val fileName = "${UUID.randomUUID()}.cbz"
        val target = comicFile(fileName)
        try {
            withContext(Dispatchers.IO) {
                comicsDir.mkdirs()
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            }
            return fileName
        } catch (e: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) { target.delete() }
            throw e
        }
    }

    /** The document's display name (e.g. `Saga #1.cbz`), or null if none is available. */
    suspend fun displayName(uri: Uri): String? = withContext(Dispatchers.IO) {
        val fromProvider = try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            // Providers may refuse the query (e.g. SecurityException); fall back to the Uri itself.
            null
        }
        fromProvider?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.takeIf { it.isNotBlank() }
    }

    fun comicFile(fileName: String): File = File(comicsDir, fileName)

    fun coverFile(fileName: String): File = File(coversDir, fileName.substringBeforeLast('.') + ".webp")

    /** Deletes a comic's stored file and cover. Files that are already gone are ignored. */
    suspend fun deleteComicFiles(fileName: String) {
        withContext(Dispatchers.IO) {
            comicFile(fileName).delete()
            coverFile(fileName).delete()
        }
    }
}

package com.androsgreen.comicreader.data.files

import android.net.Uri
import com.androsgreen.comicreader.data.archive.ComicArchiveReader
import com.androsgreen.comicreader.data.archive.InvalidArchiveException
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicFormat
import com.androsgreen.comicreader.domain.ComicImporter
import com.androsgreen.comicreader.domain.ComicRepository
import com.androsgreen.comicreader.domain.ImportFailure
import com.androsgreen.comicreader.domain.ImportResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Copy → read pages → generate cover → save. Every file is prepared before the library entry is
 * inserted, so a comic only ever appears complete and a failure only has files to clean up.
 */
class ComicImporterImpl(
    private val fileStore: ComicFileStore,
    private val archiveReader: ComicArchiveReader,
    private val coverGenerator: CoverGenerator,
    private val comicRepository: ComicRepository,
    private val clock: () -> Long = System::currentTimeMillis
) : ComicImporter {

    override suspend fun import(source: String): ImportResult {
        val uri = Uri.parse(source)
        val name = fileStore.displayName(uri) ?: UNTITLED

        val fileName = try {
            fileStore.copyIn(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // IOException, or SecurityException if access to the document was revoked.
            return ImportResult.Failed(name, ImportFailure.CANNOT_READ_FILE)
        }

        try {
            val pageCount = archiveReader.open(fileStore.comicFile(fileName)).use { archive ->
                if (archive.pageNames.isEmpty()) return fail(name, fileName, ImportFailure.NO_PAGES)
                try {
                    archive.openPage(0).use { coverGenerator.generate(it, fileStore.coverFile(fileName)) }
                } catch (e: IOException) {
                    return fail(name, fileName, ImportFailure.FIRST_PAGE_UNREADABLE)
                }
                archive.pageNames.size
            }
            val comic = Comic(
                id = 0,
                title = titleFrom(name),
                fileName = fileName,
                format = ComicFormat.CBZ,
                pageCount = pageCount,
                coverPath = fileStore.coverFile(fileName).path,
                addedAt = clock(),
                lastOpenedAt = null
            )
            // Not cancellable: a cancellation landing as the row commits would otherwise
            // trigger the cleanup below and leave a row pointing at deleted files.
            val id = withContext(NonCancellable) { comicRepository.addComic(comic) }
            return ImportResult.Imported(id, comic.title)
        } catch (e: InvalidArchiveException) {
            return fail(name, fileName, ImportFailure.NOT_A_CBZ)
        } catch (e: Throwable) {
            cleanUp(fileName)
            throw e
        }
    }

    private suspend fun fail(name: String, fileName: String, reason: ImportFailure): ImportResult.Failed {
        cleanUp(fileName)
        return ImportResult.Failed(name, reason)
    }

    private suspend fun cleanUp(fileName: String) {
        withContext(NonCancellable) { fileStore.deleteComicFiles(fileName) }
    }

    private fun titleFrom(name: String): String = name.substringBeforeLast('.').ifBlank { name }

    private companion object {
        const val UNTITLED = "Untitled"
    }
}

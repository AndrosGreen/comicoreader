package com.androsgreen.comicreader.data.files

import com.androsgreen.comicreader.data.archive.ComicArchive
import com.androsgreen.comicreader.data.archive.ComicArchiveReader
import com.androsgreen.comicreader.data.archive.ComicOpener
import com.androsgreen.comicreader.domain.Comic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Opens the copy of a comic kept in app storage by [ComicFileStore]. */
class StoredComicOpener(
    private val fileStore: ComicFileStore,
    private val archiveReader: ComicArchiveReader
) : ComicOpener {

    override suspend fun open(comic: Comic): ComicArchive = withContext(Dispatchers.IO) {
        archiveReader.open(fileStore.comicFile(comic.fileName))
    }
}

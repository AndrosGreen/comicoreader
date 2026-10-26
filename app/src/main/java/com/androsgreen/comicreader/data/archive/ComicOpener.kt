package com.androsgreen.comicreader.data.archive

import com.androsgreen.comicreader.domain.Comic

/** Opens the archive of an imported comic for reading. The caller must close it. */
fun interface ComicOpener {

    /** Throws [java.io.IOException] if the comic's stored file is missing or unreadable. */
    suspend fun open(comic: Comic): ComicArchive
}

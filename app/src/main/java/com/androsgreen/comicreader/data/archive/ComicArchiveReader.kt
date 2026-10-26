package com.androsgreen.comicreader.data.archive

import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream

interface ComicArchiveReader {

    /**
     * Opens [file] and indexes its pages. The caller must close the returned archive.
     * Throws [InvalidArchiveException] if the file is not a readable archive.
     * An archive with no pages opens normally with an empty [ComicArchive.pageNames].
     */
    fun open(file: File): ComicArchive
}

interface ComicArchive : Closeable {

    /** Page entry paths, filtered to images and in reading order. */
    val pageNames: List<String>

    /** Opens the page at [index] (0-based); the caller must close the stream. */
    fun openPage(index: Int): InputStream
}

class InvalidArchiveException(message: String, cause: Throwable? = null) : IOException(message, cause)

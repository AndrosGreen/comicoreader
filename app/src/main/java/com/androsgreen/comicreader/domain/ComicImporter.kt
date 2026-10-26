package com.androsgreen.comicreader.domain

interface ComicImporter {

    /**
     * Imports one comic from [source], a document Uri in string form. On failure nothing is
     * left behind: no stored file, no cover, no library entry. Never throws, except for
     * cancellation; unexpected errors are reported as [ImportFailure.UNEXPECTED].
     */
    suspend fun import(source: String): ImportResult
}

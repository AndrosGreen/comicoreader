package com.androsgreen.comicreader.domain

/** Outcome of creating or renaming a collection. Names are unique, case-sensitively. */
sealed interface CollectionNameResult {
    data class Success(val collectionId: Long) : CollectionNameResult
    data object NameTaken : CollectionNameResult
}

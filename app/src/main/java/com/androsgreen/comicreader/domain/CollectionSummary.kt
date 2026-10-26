package com.androsgreen.comicreader.domain

/** A collection as shown in a list: its comic count and the cover of its first comic. */
data class CollectionSummary(
    val collection: Collection,
    val comicCount: Int,
    val coverPath: String?
)

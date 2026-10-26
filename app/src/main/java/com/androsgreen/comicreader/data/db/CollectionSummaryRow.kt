package com.androsgreen.comicreader.data.db

import androidx.room.Embedded

/** A collection with its comic count and the cover of its first comic, for list rows. */
data class CollectionSummaryRow(
    @Embedded val collection: CollectionEntity,
    val comicCount: Int,
    val coverPath: String?
)

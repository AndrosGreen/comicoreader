package com.androsgreen.comicreader.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "collection_comics",
    primaryKeys = ["collectionId", "comicId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ComicEntity::class,
            parentColumns = ["id"],
            childColumns = ["comicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // collectionId is covered by the composite primary key (leftmost column); comicId is not.
    indices = [Index("comicId")]
)
data class CollectionComicEntity(
    val collectionId: Long,
    val comicId: Long,
    val position: Int
)

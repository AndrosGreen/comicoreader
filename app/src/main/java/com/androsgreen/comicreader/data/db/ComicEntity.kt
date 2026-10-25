package com.androsgreen.comicreader.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.androsgreen.comicreader.domain.ComicFormat

@Entity(tableName = "comics")
data class ComicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val fileName: String,
    // Stored as the enum name (TEXT) by Room's built-in enum support; never rename constants.
    val format: ComicFormat,
    val pageCount: Int,
    val coverPath: String?,
    val lastReadPage: Int = 0,
    val addedAt: Long,
    val lastOpenedAt: Long?
)

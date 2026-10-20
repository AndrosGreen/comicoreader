package com.androsgreen.comicreader.domain

data class Comic(
    val id: Long,
    val title: String,
    val fileName: String,
    val format: ComicFormat,
    val pageCount: Int,
    val coverPath: String?,
    val lastReadPage: Int = 0,
    val addedAt: Long,
    val lastOpenedAt: Long?
)

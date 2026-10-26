package com.androsgreen.comicreader.data.repository

import com.androsgreen.comicreader.data.db.CollectionEntity
import com.androsgreen.comicreader.data.db.CollectionSummaryRow
import com.androsgreen.comicreader.data.db.ComicEntity
import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.CollectionSummary
import com.androsgreen.comicreader.domain.Comic

fun ComicEntity.toDomain() = Comic(
    id = id,
    title = title,
    fileName = fileName,
    format = format,
    pageCount = pageCount,
    coverPath = coverPath,
    lastReadPage = lastReadPage,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt
)

fun Comic.toEntity() = ComicEntity(
    id = id,
    title = title,
    fileName = fileName,
    format = format,
    pageCount = pageCount,
    coverPath = coverPath,
    lastReadPage = lastReadPage,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt
)

fun CollectionEntity.toDomain() = Collection(
    id = id,
    name = name,
    createdAt = createdAt
)

fun Collection.toEntity() = CollectionEntity(
    id = id,
    name = name,
    createdAt = createdAt
)

fun CollectionSummaryRow.toDomain() = CollectionSummary(
    collection = collection.toDomain(),
    comicCount = comicCount,
    coverPath = coverPath
)

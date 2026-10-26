package com.androsgreen.comicreader.data.repository

import com.androsgreen.comicreader.data.db.CollectionEntity
import com.androsgreen.comicreader.data.db.ComicEntity
import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    // Every field has a distinct value, so a swapped or dropped field shows up as a mismatch.
    private val comicEntity = ComicEntity(
        id = 1L,
        title = "Saga #1",
        fileName = "3f2a.cbz",
        format = ComicFormat.CBZ,
        pageCount = 44,
        coverPath = "/covers/1.webp",
        lastReadPage = 12,
        addedAt = 1_000L,
        lastOpenedAt = 2_000L
    )

    private val comic = Comic(
        id = 1L,
        title = "Saga #1",
        fileName = "3f2a.cbz",
        format = ComicFormat.CBZ,
        pageCount = 44,
        coverPath = "/covers/1.webp",
        lastReadPage = 12,
        addedAt = 1_000L,
        lastOpenedAt = 2_000L
    )

    @Test
    fun comicEntity_toDomain_mapsEveryField() {
        assertEquals(comic, comicEntity.toDomain())
    }

    @Test
    fun comic_toEntity_mapsEveryField() {
        assertEquals(comicEntity, comic.toEntity())
    }

    @Test
    fun comic_roundTrip_withNullFields() {
        val neverOpened = comic.copy(coverPath = null, lastOpenedAt = null, lastReadPage = 0)

        assertEquals(neverOpened, neverOpened.toEntity().toDomain())
    }

    @Test
    fun collectionEntity_toDomain_mapsEveryField() {
        assertEquals(
            Collection(id = 3L, name = "Marvel", createdAt = 5_000L),
            CollectionEntity(id = 3L, name = "Marvel", createdAt = 5_000L).toDomain()
        )
    }

    @Test
    fun collection_roundTrip() {
        val collection = Collection(id = 3L, name = "Marvel", createdAt = 5_000L)

        assertEquals(collection, collection.toEntity().toDomain())
    }
}

package com.androsgreen.comicreader.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androsgreen.comicreader.data.files.ComicImporterImpl
import com.androsgreen.comicreader.data.repository.CollectionRepositoryImpl
import com.androsgreen.comicreader.data.repository.ComicRepositoryImpl
import com.androsgreen.comicreader.domain.CollectionRepository
import com.androsgreen.comicreader.domain.ComicImporter
import com.androsgreen.comicreader.domain.ComicRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Resolves the data layer from the Koin graph the app's Application started, so a missing or
 * mis-typed definition fails here rather than on first use. Koin `single`s are lazy, so the app
 * launching is not enough to prove this.
 */
@RunWith(AndroidJUnit4::class)
class AppModuleTest {

    private val koin = GlobalContext.get()

    @Test
    fun resolvesComicRepository() {
        assertTrue(koin.get<ComicRepository>() is ComicRepositoryImpl)
    }

    @Test
    fun resolvesCollectionRepository() {
        assertTrue(koin.get<CollectionRepository>() is CollectionRepositoryImpl)
    }

    @Test
    fun resolvesComicImporter() {
        assertTrue(koin.get<ComicImporter>() is ComicImporterImpl)
    }
}

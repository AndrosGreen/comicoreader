package com.androsgreen.comicreader.di

import androidx.room.Room
import com.androsgreen.comicreader.data.archive.CbzReader
import com.androsgreen.comicreader.data.archive.ComicArchiveReader
import com.androsgreen.comicreader.data.db.AppDatabase
import com.androsgreen.comicreader.data.files.ComicFileStore
import com.androsgreen.comicreader.data.files.ComicImporterImpl
import com.androsgreen.comicreader.data.files.CoverGenerator
import com.androsgreen.comicreader.data.repository.CollectionRepositoryImpl
import com.androsgreen.comicreader.data.repository.ComicRepositoryImpl
import com.androsgreen.comicreader.domain.CollectionRepository
import com.androsgreen.comicreader.domain.ComicImporter
import com.androsgreen.comicreader.domain.ComicRepository
import com.androsgreen.comicreader.ui.collections.CollectionDetailViewModel
import com.androsgreen.comicreader.ui.collections.CollectionsViewModel
import com.androsgreen.comicreader.ui.library.LibraryViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Application-wide Koin definitions.
 */
val appModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "comic_reader.db").build()
    }
    single { get<AppDatabase>().comicDao() }
    single { get<AppDatabase>().collectionDao() }

    single<ComicRepository> { ComicRepositoryImpl(get(), get()) }
    single<CollectionRepository> { CollectionRepositoryImpl(get()) }

    single { ComicFileStore(androidContext().contentResolver, androidContext().filesDir) }
    single<ComicArchiveReader> { CbzReader() }
    single { CoverGenerator() }
    single<ComicImporter> { ComicImporterImpl(get(), get(), get(), get()) }

    viewModel { LibraryViewModel(comicRepository = get(), collectionRepository = get(), importer = get()) }
    viewModel { CollectionsViewModel(get()) }
    viewModel { params -> CollectionDetailViewModel(collectionId = params.get(), repository = get()) }
}

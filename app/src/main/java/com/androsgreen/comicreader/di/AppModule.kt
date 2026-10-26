package com.androsgreen.comicreader.di

import androidx.room.Room
import com.androsgreen.comicreader.data.db.AppDatabase
import com.androsgreen.comicreader.data.repository.CollectionRepositoryImpl
import com.androsgreen.comicreader.data.repository.ComicRepositoryImpl
import com.androsgreen.comicreader.domain.CollectionRepository
import com.androsgreen.comicreader.domain.ComicRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Application-wide Koin definitions. View models are added from M2 onwards.
 */
val appModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "comic_reader.db").build()
    }
    single { get<AppDatabase>().comicDao() }
    single { get<AppDatabase>().collectionDao() }

    single<ComicRepository> { ComicRepositoryImpl(get()) }
    single<CollectionRepository> { CollectionRepositoryImpl(get()) }
}

package com.androsgreen.comicreader

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.androsgreen.comicreader.di.appModule
import com.androsgreen.comicreader.ui.reader.ComicPageFetcher
import com.androsgreen.comicreader.ui.reader.ComicPageKeyer
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ComicReaderApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ComicReaderApplication)
            modules(appModule)
        }
    }

    /** The app-wide Coil loader, able to load [com.androsgreen.comicreader.ui.reader.ComicPage]s. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(ComicPageKeyer())
                add(ComicPageFetcher.Factory())
            }
            .build()
}

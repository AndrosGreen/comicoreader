package com.androsgreen.comicreader.ui.reader

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.androsgreen.comicreader.data.archive.ComicArchive
import okio.buffer
import okio.source

/**
 * A Coil model for one page of an open [archive]. The reader keeps a single archive open for the
 * whole session, and Coil reads pages from it on demand, decoding each at the requested size.
 * [fileName] identifies the comic for the memory-cache key.
 */
data class ComicPage(
    val archive: ComicArchive,
    val fileName: String,
    val index: Int
)

class ComicPageFetcher(
    private val page: ComicPage,
    private val options: Options
) : Fetcher {

    // Coil runs fetchers off the main thread; the stream is read lazily while decoding.
    override suspend fun fetch(): FetchResult = SourceFetchResult(
        source = ImageSource(page.archive.openPage(page.index).source().buffer(), options.fileSystem),
        mimeType = null,
        dataSource = DataSource.DISK
    )

    class Factory : Fetcher.Factory<ComicPage> {
        override fun create(data: ComicPage, options: Options, imageLoader: ImageLoader): Fetcher =
            ComicPageFetcher(data, options)
    }
}

/** Stable per comic file and page, so a page decoded once is reused while paging back and forth. */
class ComicPageKeyer : Keyer<ComicPage> {
    override fun key(data: ComicPage, options: Options): String = "comic-page:${data.fileName}:${data.index}"
}

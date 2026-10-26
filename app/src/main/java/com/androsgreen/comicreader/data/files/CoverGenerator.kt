package com.androsgreen.comicreader.data.files

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Turns a comic page into a small WebP cover thumbnail. The page is decoded straight to the
 * thumbnail size, so a full-resolution page is never held in memory.
 */
class CoverGenerator(
    private val maxWidth: Int = 600,
    private val maxHeight: Int = 900,
    private val quality: Int = 80
) {

    /**
     * Writes a cover for [page] to [target], fitting inside [maxWidth] x [maxHeight] without
     * upscaling. Throws [IOException] if the page cannot be decoded; no partial file is left.
     */
    suspend fun generate(page: InputStream, target: File) {
        withContext(Dispatchers.IO) {
            // Zip entry streams can't be re-read, so decode from the page's compressed bytes.
            val source = ImageDecoder.createSource(ByteBuffer.wrap(page.readBytes()))
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val scale = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height, 1f)
                decoder.setTargetSize(
                    (width * scale).roundToInt().coerceAtLeast(1),
                    (height * scale).roundToInt().coerceAtLeast(1)
                )
                // Hardware bitmaps cannot be compressed.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val temp = File(target.path + ".tmp")
            try {
                target.parentFile?.mkdirs()
                temp.outputStream().use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, output)) {
                        throw IOException("Could not encode cover")
                    }
                }
                if (!temp.renameTo(target)) throw IOException("Could not save cover to $target")
            } catch (e: Throwable) {
                temp.delete()
                throw e
            } finally {
                bitmap.recycle()
            }
        }
    }
}

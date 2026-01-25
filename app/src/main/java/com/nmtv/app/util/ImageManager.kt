package com.nmtv.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.widget.ImageView
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages image loading and caching for the app.
 * Uses Coil for efficient image loading with memory caching.
 *
 * Features:
 * - LRU cache for blurred background images
 * - Efficient bitmap handling
 * - Proper resource cleanup
 */
class ImageManager(private val context: Context) {

    private val imageLoader: ImageLoader = ImageLoader.Builder(context)
        .crossfade(true)
        .build()

    // Cache for blurred versions of images (key: resource ID)
    private val blurredBitmapCache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(
        // Use 1/8th of available memory for cache
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            // Size in kilobytes
            return bitmap.byteCount / 1024
        }

        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            // Recycle old bitmap if evicted and not the same as new value
            if (evicted && oldValue != newValue && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ImageManager? = null

        fun getInstance(context: Context): ImageManager {
            return instance ?: synchronized(this) {
                instance ?: ImageManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Load an image into an ImageView using Coil.
     *
     * @param imageView Target ImageView
     * @param resourceId Drawable resource ID
     * @param crossfade Whether to use crossfade animation
     */
    fun loadImage(imageView: ImageView, resourceId: Int, crossfade: Boolean = true) {
        val request = ImageRequest.Builder(context)
            .data(resourceId)
            .target(imageView)
            .crossfade(crossfade)
            .scale(Scale.FILL)
            .build()

        imageLoader.enqueue(request)
    }

    /**
     * Load an image from URL into an ImageView.
     *
     * @param imageView Target ImageView
     * @param url Image URL
     * @param placeholder Placeholder drawable resource
     */
    fun loadImageFromUrl(imageView: ImageView, url: String, placeholder: Int? = null) {
        val requestBuilder = ImageRequest.Builder(context)
            .data(url)
            .target(imageView)
            .crossfade(true)
            .scale(Scale.FILL)

        placeholder?.let { requestBuilder.placeholder(it) }

        imageLoader.enqueue(requestBuilder.build())
    }

    /**
     * Get a bitmap for a drawable resource.
     * Uses caching for efficiency.
     *
     * @param resourceId Drawable resource ID
     * @return Bitmap or null if failed
     */
    suspend fun getBitmap(resourceId: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(resourceId)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get or create a blurred version of a resource bitmap.
     * Cached for reuse.
     *
     * @param resourceId Drawable resource ID
     * @return Cached or newly created blurred bitmap
     */
    fun getCachedBlurredBitmap(resourceId: Int): Bitmap? {
        return blurredBitmapCache.get(resourceId)
    }

    /**
     * Cache a blurred bitmap for a resource ID.
     *
     * @param resourceId Drawable resource ID
     * @param bitmap Blurred bitmap to cache
     */
    fun cacheBlurredBitmap(resourceId: Int, bitmap: Bitmap) {
        blurredBitmapCache.put(resourceId, bitmap)
    }

    /**
     * Get the bitmap from an ImageView's drawable.
     *
     * @param imageView Source ImageView
     * @return Bitmap or null if not a BitmapDrawable
     */
    fun getBitmapFromImageView(imageView: ImageView): Bitmap? {
        return (imageView.drawable as? BitmapDrawable)?.bitmap
    }

    /**
     * Clear all caches and release resources.
     * Call when the app is being destroyed.
     */
    fun clearCache() {
        blurredBitmapCache.evictAll()
    }

    /**
     * Get current cache size info for debugging.
     */
    fun getCacheInfo(): String {
        return "BlurCache: ${blurredBitmapCache.size()}/${blurredBitmapCache.maxSize()} KB"
    }
}

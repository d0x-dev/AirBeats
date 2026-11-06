package com.darkxvenom.airbeats

import android.graphics.Bitmap
import android.util.LruCache

internal object WidgetArtworkCache {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    fun get(url: String, width: Int, height: Int): Bitmap? = cache.get(key(url, width, height))

    fun put(url: String, width: Int, height: Int, bitmap: Bitmap) {
        cache.put(key(url, width, height), bitmap)
    }

    private fun key(url: String, width: Int, height: Int) = "$url|${width}x$height"
}

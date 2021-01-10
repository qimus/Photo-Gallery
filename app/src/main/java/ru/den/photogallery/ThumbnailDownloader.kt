package ru.den.photogallery

import android.graphics.Bitmap
import android.os.Handler
import android.util.Log
import androidx.collection.LruCache
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import ru.den.photogallery.api.repository.FlickrRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "ThumbnailDownloader"

class ThumbnailDownloader<in T>(
    private val responseHandler: Handler,
    private val onThumbnailDownloaded: (T, Bitmap) -> Unit
) {
    private var hasQuit = false
    private val requestMap = ConcurrentHashMap<T, String>()
    private val flickrRepository = FlickrRepository()
    private val cache = LruCache<String, Bitmap>(100)
    private var executorService: ExecutorService? = null

    val fragmentLifecycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun setup() {
            Log.i(TAG, "Starting background thread - ${Thread.currentThread().name}")
            executorService = Executors.newFixedThreadPool(4)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun tearDown() {
            Log.i(TAG, "Destroying background thread - ${Thread.currentThread().name}")
            quit()
        }
    }

    val viewLifecycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun clearQueue() {
            Log.i(TAG, "Clearing all requests from queue")
            requestMap.clear()
        }
    }

    private fun quit() {
        hasQuit = true
        executorService?.shutdown()
        executorService = null
    }

    private fun handleRequest(target: T) {
        val url = requestMap[target] ?: return
        var bitmap: Bitmap? = cache.get(url)
        if (bitmap == null) {
            Log.i(TAG, "Fetching url - $url")
            bitmap = flickrRepository.fetchPhoto(url) ?: return
            cache.put(url, bitmap)
        } else {
            Log.i(TAG, "Get from cache - $url")
        }

        responseHandler.post {
            if (requestMap[target] != url || hasQuit) {
                return@post
            }

            requestMap.remove(target)
            onThumbnailDownloaded.invoke(target, bitmap)
        }
    }

    private fun handlePreupload(url: String) {
        if (cache.get(url) == null) {
            val bitmap = flickrRepository.fetchPhoto(url) ?: return
            cache.put(url, bitmap)
        }
    }

    fun queueThumbnail(target: T, url: String) {
        requestMap[target] = url
        executorService?.submit {
            handleRequest(target)
        }
    }

    fun preupload(url: String) {
        executorService?.submit {
            handlePreupload(url)
        }
    }
}

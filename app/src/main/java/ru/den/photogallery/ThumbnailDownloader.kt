package ru.den.photogallery

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.collection.LruCache
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import ru.den.photogallery.api.FlickrRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

private const val TAG = "ThumbnailDownloader"
private const val MESSAGE_DOWNLOAD = 0
private const val MESSAGE_PREUPLOAD = 1

class ThumbnailDownloader<in T>(
    private val responseHandler: Handler,
    private val onThumbnailDownloaded: (T, Bitmap) -> Unit
) : HandlerThread(TAG) {
    private var hasQuit = false
    private lateinit var requestHandler: Handler
    private val requestMap = ConcurrentHashMap<T, String>()
    private val flickrRepository = FlickrRepository()
    private val cache = LruCache<String, Bitmap>(100)

    private val executorService = Executors.newFixedThreadPool(4)

    val fragmentLifecycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun setup() {
            Log.i(TAG, "Starting background thread - ${Thread.currentThread().name}")
            start()
            looper
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
            requestHandler.removeMessages(MESSAGE_DOWNLOAD)
            requestMap.clear()
        }
    }

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        requestHandler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    val target = msg.obj as T
                    handleRequest(target)
                } else if (msg.what == MESSAGE_PREUPLOAD) {
                    handlePreupload(msg.obj as String)
                }
            }
        }
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
        requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget()
    }

    fun preupload(url: String) {
        requestHandler.obtainMessage(MESSAGE_PREUPLOAD, url).sendToTarget()
    }
}

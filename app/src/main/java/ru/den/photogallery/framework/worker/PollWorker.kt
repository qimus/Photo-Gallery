package ru.den.photogallery.framework.worker

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import ru.den.photogallery.QueryPreferences
import ru.den.photogallery.api.repository.FlickrRepository
import ru.den.photogallery.model.GalleryItem
import java.util.concurrent.TimeUnit

private const val TAG = "PollWorker"
private const val POLL_WORK = "POLL_WORK"

class PollWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    private val pollNotification by lazy { PollNotification(context) }

    override fun doWork(): Result {
        val query = QueryPreferences.getStoredQuery(context)
        val lastResultId = QueryPreferences.getLastResultId(context)
        val items: List<GalleryItem> = if (query.isEmpty()) {
            FlickrRepository().fetchPhotosRequest()
                .execute().body()?.photos?.galleryItems
        } else {
            FlickrRepository().searchPhotosRequest(query)
                .execute().body()?.photos?.galleryItems
        } ?: emptyList()

        if (items.isEmpty()) {
            return Result.success()
        }

        val resultId = items.first().id
        if (resultId == lastResultId) {
            Log.i(TAG, "Got an old result: $resultId")
        } else {
            Log.i(TAG, "Got a new result: $resultId")
            QueryPreferences.setLastResultId(context, resultId)
        }

        showBackgroundNotification(0, pollNotification.buildNotification())

        return Result.success()
    }

    private fun showBackgroundNotification(requestCode: Int, notification: Notification) {
        val intent = Intent(ACTION_SHOW_NOTIFICATION).apply {
            putExtra(REQUEST_CODE, requestCode)
            putExtra(NOTIFICATION, notification)
        }
        context.sendOrderedBroadcast(intent, PERM_PRIVATE)
    }

    companion object {
        const val ACTION_SHOW_NOTIFICATION = "ru.den.photogallery.ACTION_SHOW_NOTIFICATION"
        const val PERM_PRIVATE = "ru.den.photogallery.permission.PRIVATE"
        const val REQUEST_CODE = "REQUEST_CODE"
        const val NOTIFICATION = "NOTIFICATION"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()

           val periodicRequest = PeriodicWorkRequest
               .Builder(PollWorker::class.java, 15, TimeUnit.MINUTES)
               .setConstraints(constraints)
               .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(POLL_WORK,
                    ExistingPeriodicWorkPolicy.REPLACE, periodicRequest)
            QueryPreferences.setIsPolling(context, true)
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(POLL_WORK)
            QueryPreferences.setIsPolling(context, false)
        }
    }
}

package ru.den.photogallery.framework.lifecycle

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import ru.den.photogallery.framework.worker.PollWorker

class NotificationObserver(private val context: Context) : LifecycleObserver {
    private val onShowNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i("NotificationObserver", "canceling notification")
            resultCode = Activity.RESULT_CANCELED
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        val filter = IntentFilter()
        filter.addAction(PollWorker.ACTION_SHOW_NOTIFICATION)
        context.registerReceiver(onShowNotification, filter, PollWorker.PERM_PRIVATE, null)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        context.unregisterReceiver(onShowNotification)
    }
}

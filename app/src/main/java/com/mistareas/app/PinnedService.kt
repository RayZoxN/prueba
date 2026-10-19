package com.mistareas.app

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

/** Servicio en primer plano: mantiene viva la notificacion con tus tareas. */
class PinnedService : Service() {

    companion object {
        @Volatile
        var running = false
    }

    // Cuando cambia el dia o la hora, se actualiza lo que es "hoy".
    private val clockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            PinnedNotifier.post(context)
            TaskWidget.refreshAll(context)
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        PinnedNotifier.ensureChannel(this)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(this, clockReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = PinnedNotifier.build(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                PinnedNotifier.ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(PinnedNotifier.ID, notification)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        try {
            unregisterReceiver(clockReceiver)
        } catch (e: Exception) {
            // ya estaba desregistrado
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

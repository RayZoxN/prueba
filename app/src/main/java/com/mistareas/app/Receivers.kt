package com.mistareas.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Al encender el telefono (o actualizar la app) vuelve a mostrar notificacion y widget. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TaskWidget.refreshAll(context)
        if (TaskStore.isPinned(context)) {
            PinnedNotifier.start(context)
        }
    }
}

/** Se ejecuta cuando alguien desliza la notificacion fija: la vuelve a poner. */
class PinnedRepostReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (TaskStore.isPinned(context)) {
            PinnedNotifier.post(context)
        }
    }
}

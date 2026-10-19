package com.mistareas.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** Construye y publica la notificacion fija con las tareas del dia y las proximas. */
object PinnedNotifier {
    const val CHANNEL = "tareas_fijas"
    const val ID = 1001
    const val ACTION_REPOST = "com.mistareas.app.REPOST"

    private fun manager(c: Context) =
        c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel(c: Context) {
        val ch = NotificationChannel(CHANNEL, "Tareas fijas", NotificationManager.IMPORTANCE_LOW)
        ch.description = "Muestra tus tareas pendientes de forma permanente"
        ch.setShowBadge(false)
        manager(c).createNotificationChannel(ch)
    }

    fun build(c: Context): Notification {
        ensureChannel(c)
        val today = TaskStore.dueToday(c)
        val upcoming = TaskStore.upcoming(c)

        val title = when {
            today.isEmpty() -> "Hoy: nada pendiente"
            today.size == 1 -> "Hoy: 1 pendiente"
            else -> "Hoy: ${today.size} pendientes"
        }
        val text = when {
            today.isNotEmpty() -> today.first().title
            upcoming.isNotEmpty() -> "Próximo: ${upcoming.first().title} (${upcoming.first().dateLabel()})"
            else -> "Toca para agregar una tarea"
        }

        val inbox = NotificationCompat.InboxStyle().setBigContentTitle(title)
        today.take(5).forEach { inbox.addLine("• " + it.title) }
        if (today.size > 5) inbox.addLine("… y ${today.size - 5} más hoy")
        upcoming.take(3).forEach { inbox.addLine("Próximo: ${it.title} · ${it.dateLabel()}") }
        if (today.isEmpty() && upcoming.isEmpty()) inbox.addLine("Sin tareas pendientes")

        val openApp = PendingIntent.getActivity(
            c, 0,
            Intent(c, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val addTask = PendingIntent.getActivity(
            c, 1,
            Intent(c, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_ADD, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Si alguien desliza la notificacion (Android 14+ lo permite), se vuelve a publicar.
        val repost = PendingIntent.getBroadcast(
            c, 2,
            Intent(c, PinnedRepostReceiver::class.java).setAction(ACTION_REPOST),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(inbox)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp)
            .setDeleteIntent(repost)
            .addAction(0, "Agregar tarea", addTask)
            .build()
    }

    /** Publica o actualiza la notificacion sin tocar el servicio. */
    fun post(c: Context) {
        manager(c).notify(ID, build(c))
    }

    /** Actualiza la notificacion si esta activada. */
    fun refresh(c: Context) {
        if (!TaskStore.isPinned(c)) return
        if (PinnedService.running) post(c) else start(c)
    }

    fun start(c: Context) {
        try {
            ContextCompat.startForegroundService(c, Intent(c, PinnedService::class.java))
        } catch (e: Exception) {
            // Android puede negar iniciar el servicio desde segundo plano; se publica normal.
            post(c)
        }
    }

    fun stop(c: Context) {
        c.stopService(Intent(c, PinnedService::class.java))
        manager(c).cancel(ID)
    }
}
